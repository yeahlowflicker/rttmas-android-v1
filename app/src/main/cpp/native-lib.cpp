#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>

#include "src/yolo11.h"


#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

// Deterministic class count for each YOLO model
const int CLASS_COUNT_LICENSE_PLATE = 1;
const int CLASS_COUNT_PARKING_SLOT = 6;

static Yolo *g_license_plate = 0;
static ncnn::Mutex lock_license_plate;

static Yolo *g_parking_slot = 0;
static ncnn::Mutex lock_parking_slot;

cv::Mat bitmapToMat(JNIEnv *env, jobject bitmap) {
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);

    void *pixels = nullptr;
    AndroidBitmap_lockPixels(env, bitmap, &pixels);

    cv::Mat mat(info.height, info.width, CV_8UC4, pixels);

    cv::cvtColor(mat, mat, cv::COLOR_RGBA2BGR);

    AndroidBitmap_unlockPixels(env, bitmap);

    return mat.clone();
}


extern "C"
{
static jclass objCls = NULL;
static jmethodID constructortorId;
static jfieldID xId;
static jfieldID yId;
static jfieldID wId;
static jfieldID hId;
static jfieldID labelId;
static jfieldID probId;


JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnLoad");

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");
    {
        ncnn::MutexLockGuard g1(lock_license_plate);
        ncnn::MutexLockGuard g2(lock_parking_slot);

        delete g_license_plate;
        delete g_parking_slot;
        g_license_plate = 0;
        g_parking_slot = 0;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_mwnl_rttmas_1android_services_YoloService_loadLicensePlateModel(JNIEnv *env, jobject thiz,
                                                          jobject assetManager,
                                                          jint modelid, jint cpugpu) {
    if (modelid < 0 || cpugpu < 0 || cpugpu > 1) {
        return JNI_FALSE;
    }

    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);

    const char *modeltypes[] =
            {
                    "rttmas_license_plates_20250120",
            };

    const char *modeltype = modeltypes[(int) modelid];

    bool use_gpu = (int) cpugpu == 1;

    // reload
    {
        ncnn::MutexLockGuard g(lock_license_plate);

        if (use_gpu && ncnn::get_gpu_count() == 0) {
            // no gpu
            delete g_license_plate;
            g_license_plate = 0;
        } else {
            if (!g_license_plate)
                g_license_plate = new Yolo;
            __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "load model %s", modeltype);

            g_license_plate->load(mgr, modeltype, use_gpu);
        }
    }

    // init jni glue
    jclass localObjCls = env->FindClass("com/mwnl/rttmas_android/services/YoloService$Obj");
    objCls = reinterpret_cast<jclass>(env->NewGlobalRef(localObjCls));

    constructortorId = env->GetMethodID(objCls, "<init>", "()V");

    xId = env->GetFieldID(objCls, "x", "F");
    yId = env->GetFieldID(objCls, "y", "F");
    wId = env->GetFieldID(objCls, "w", "F");
    hId = env->GetFieldID(objCls, "h", "F");
    labelId = env->GetFieldID(objCls, "label", "I");
    probId = env->GetFieldID(objCls, "prob", "F");

    return JNI_TRUE;
}


JNIEXPORT jboolean JNICALL
Java_com_mwnl_rttmas_1android_services_YoloService_loadParkingSlotModel(JNIEnv *env, jobject thiz,
                                                             jobject assetManager,
                                                             jint modelid, jint cpugpu) {
    if (modelid < 0 || cpugpu < 0 || cpugpu > 1) {
        return JNI_FALSE;
    }

    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);

    const char *modeltypes[] =
            {
                    "rttmas_parking_slot_v3",
            };

    const char *modeltype = modeltypes[(int) modelid];

    bool use_gpu = (int) cpugpu == 1;

    // reload
    {
        ncnn::MutexLockGuard g(lock_parking_slot);

        if (use_gpu && ncnn::get_gpu_count() == 0) {
            // no gpu
            delete g_parking_slot;
            g_parking_slot = 0;
        } else {
            if (!g_parking_slot)
                g_parking_slot = new Yolo;
            __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "load model %s", modeltype);

            g_parking_slot->load(mgr, modeltype, use_gpu);
        }
    }

    // init jni glue
    jclass localObjCls = env->FindClass("com/mwnl/rttmas_android/services/YoloService$Obj");
    objCls = reinterpret_cast<jclass>(env->NewGlobalRef(localObjCls));

    constructortorId = env->GetMethodID(objCls, "<init>", "()V");

    xId = env->GetFieldID(objCls, "x", "F");
    yId = env->GetFieldID(objCls, "y", "F");
    wId = env->GetFieldID(objCls, "w", "F");
    hId = env->GetFieldID(objCls, "h", "F");
    labelId = env->GetFieldID(objCls, "label", "I");
    probId = env->GetFieldID(objCls, "prob", "F");

    return JNI_TRUE;
}


int imgWidth;
int imgHeight;
JNIEXPORT jobjectArray JNICALL
Java_com_mwnl_rttmas_1android_services_YoloService_detect(JNIEnv *env, jobject thiz, jobject bitmap) {
    cv::Mat img = bitmapToMat(env, bitmap);

    imgWidth = img.cols;
    imgHeight = img.rows;
    std::vector<DetectRes> plateObjects, parkingObjects;
    
    plateObjects = g_license_plate->detect(img, 0.4, 0.5);
    parkingObjects = g_parking_slot->detect(img, 0.4, 0.5);

    size_t plateCount = plateObjects.size();
    size_t parkingCount = parkingObjects.size();

    size_t totalObjectCount = plateCount+parkingCount;

    jobjectArray jObjArray = env->NewObjectArray(totalObjectCount, objCls, NULL);
    
    for (size_t i = 0; i < plateObjects.size(); i++) {
        jobject jObj = env->NewObject(objCls, constructortorId);

        env->SetFloatField(jObj, xId, plateObjects[i].rect.x);
        env->SetFloatField(jObj, yId, plateObjects[i].rect.y);
        env->SetFloatField(jObj, wId, plateObjects[i].rect.width);
        env->SetFloatField(jObj, hId, plateObjects[i].rect.height);
        env->SetIntField(jObj, labelId, plateObjects[i].label);
        env->SetFloatField(jObj, probId, plateObjects[i].prob);

        env->SetObjectArrayElement(jObjArray, i, jObj);
    }


    for (size_t i = 0; i < parkingObjects.size(); i++) {
        jobject jObj = env->NewObject(objCls, constructortorId);

        env->SetFloatField(jObj, xId, parkingObjects[i].rect.x);
        env->SetFloatField(jObj, yId, parkingObjects[i].rect.y);
        env->SetFloatField(jObj, wId, parkingObjects[i].rect.width);
        env->SetFloatField(jObj, hId, parkingObjects[i].rect.height);
        env->SetIntField(jObj, labelId, parkingObjects[i].label + CLASS_COUNT_LICENSE_PLATE);
        env->SetFloatField(jObj, probId, parkingObjects[i].prob);

        env->SetObjectArrayElement(jObjArray, plateCount+i, jObj);
    }

    return jObjArray;
}

}
