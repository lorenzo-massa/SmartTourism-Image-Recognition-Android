#include <jni.h>
#include <string>

#include <cmath>
#include <cstdio>
#include <cstdlib>

#include <sys/time.h>
#include <linux/stat.h>
#include <iosfwd>
#include <fstream>

#include "faiss/IndexIVFPQ.h"
#include "faiss/IndexFlat.h"
#include "faiss/index_io.h"

#include "log.h"

int64_t getCurrentMillTime() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return ((int64_t) tv.tv_sec * 1000 + (int64_t) tv.tv_usec / 1000);//毫秒
}

faiss::IndexFlatL2 *index; //global variable

extern "C" JNIEXPORT jstring

JNICALL stringFromJNI(JNIEnv *env, jclass clazz,jfloatArray imgFeatures,jobjectArray data,jint k) {

    std::string result = "";

    jfloat *bodyImgFeatures = (*env).GetFloatArrayElements(imgFeatures, 0);
    jsize len = (*env).GetArrayLength(data);


    //CREATING INDEX in not already done

    if (index == nullptr){

        jsize d = (*env).GetArrayLength(imgFeatures); //features number
        index = new faiss::IndexFlatL2(d);

        //ADD TO TE INDEX
        for(int i = 0; i < len; i++) {
            jfloatArray arr = (jfloatArray) (*env).GetObjectArrayElement(data, i);
            jsize innerLen = (*env).GetArrayLength(arr);
            jfloat* vals = (*env).GetFloatArrayElements(arr, NULL);

            index->add(1, vals);

            (*env).ReleaseFloatArrayElements(arr, vals, JNI_COMMIT);
            (*env).DeleteLocalRef(arr);

            delete(vals);
        }
    }
    //SEARCHING
    const int64_t destCount = k; //k

    //result vectors
    int64_t *listIndex = (int64_t *) malloc(sizeof(int64_t) * destCount);
    float *listScore = (float *) malloc(sizeof(float) * destCount);

    //seraching
    index->search(1, bodyImgFeatures, destCount, listScore, listIndex);

    for (int i = 0; i < destCount; i++) {
        //LOGI("index->search[%lld]=%f", listIndex[i], listScore[i]);
        result += std::to_string(listIndex[i]) + " "+std::to_string(listScore[i]) + " ";
    }

    delete(listIndex);
    delete(listScore);
    delete(bodyImgFeatures);
    //delete(index);

    //result = std::to_string(index->ntotal);

    return env->NewStringUTF(result.c_str());
}

#define JNIREG_CLASS_BASE "org/tensorflow/lite/examples/classification/tflite/Retrievor"
static JNINativeMethod gMethods_Base[] = {
        {"stringFromJNI", "([F[[FI)Ljava/lang/String;", (void *) stringFromJNI}, //(...) input e out return type
};

static int registerNativeMethods(JNIEnv *env, const char *className,
                                 JNINativeMethod *gMethods, int numMethods) {
    jclass clazz;
    clazz = (*env).FindClass(className);
    if (clazz == nullptr) {
        return JNI_FALSE;
    }
    if ((*env).RegisterNatives(clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}


static int registerNatives(JNIEnv *env) {
    if (!registerNativeMethods(env, JNIREG_CLASS_BASE, gMethods_Base,
                               sizeof(gMethods_Base) / sizeof(gMethods_Base[0]))) {
        return JNI_FALSE;
    }


    return JNI_TRUE;
}


JNIEXPORT jint

JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGI("JNI_OnLoad");
    JNIEnv *env = nullptr;
    jint result = -1;

    if ((*vm).GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    assert(env != nullptr);

    if (!registerNatives(env)) { //注册
        return -1;
    }

    result = JNI_VERSION_1_4;
    return result;

}


JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {


}

