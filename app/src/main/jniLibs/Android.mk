LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#OPENCV_INSTALL_MODULES:=off
#OPENCV_LIB_TYPE:=SHARED
ifdef OPENCV_ANDROID_SDK
  ifneq ("","$(wildcard $(OPENCV_ANDROID_SDK)/OpenCV.mk)")
    include ${OPENCV_ANDROID_SDK}/OpenCV.mk
  else
    include ${OPENCV_ANDROID_SDK}/sdk/native/jniLibs/OpenCV.mk
  endif
else
  ifneq ("","$(wildcard $(LOCAL_PATH)/OpenCV.mk)")
    include ${LOCAL_PATH}/OpenCV.mk
  else
    include ../../sdk/native/jniLibs/OpenCV.mk
  endif
endif

LOCAL_SRC_FILES  := DetectionBasedTracker_jni.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS     += -L$(LOCAL_PATH)/$(TARGET_ARCH_ABI) -llog -ldl

LOCAL_MODULE     := detection_based_tracker

include $(BUILD_SHARED_LIBRARY)
