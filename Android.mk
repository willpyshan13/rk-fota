LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files) 
# LOCAL_SRC_FILES := $(call all-java-files-under, src)

# Also link against our own custom library.
# LOCAL_JAVA_LIBRARIES := framework
	
LOCAL_PACKAGE_NAME := FOTA
LOCAL_JNI_SHARED_LIBRARIES := librockchip_update_jni
LOCAL_REQUIRED_MODULES := librockchip_update_jni
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4
LOCAL_STATIC_JAVA_LIBRARIES += ftp4j-1.7.2
LOCAL_STATIC_JAVA_LIBRARIES += http
#LOCAL_CERTIFICATE := media
LOCAL_OVERRIDES_PACKAGES := RKUpdateService
LOCAL_CERTIFICATE := platform
LOCAL_OVERRIDES_PACKAGES := RKUpdateService
LOCAL_PROGUARD_ENABLED := disabled
include $(BUILD_PACKAGE)

include $(CLEAR_VARS) 

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := ftp4j-1.7.2:/libs/ftp4j-1.7.2.jar
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += ota_httpclient:libs/httpclient-4.5.2.jar
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += ota_httpcore:libs/httpcore-4.4.4.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += http:libs/org.apache.http.legacy.jar

include $(BUILD_MULTI_PREBUILT)
# ============================================================

# Also build all of the sub-targets under this one: the shared library.
include $(call all-makefiles-under,$(LOCAL_PATH))
