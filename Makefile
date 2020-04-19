MAKE_SUBDIRS = datagramSocket_C jni
CURRENT_DIR = $(shell pwd)
CLASS_PATH=$(CURRENT_DIR)/bin
SRC_PATH=$(CURRENT_DIR)/src
JNI_PATH=$(CURRENT_DIR)/jni
NATIVE_CLASS_NAMES=ReceiveTools.UdpSocket

.PHONY: $(MAKE_SUBDIRS)

all: $(MAKE_SUBDIRS)
	javac $$(find $(SRC_PATH) -name "*.java") -d $(CLASS_PATH) -Xlint
	
	for d in $(NATIVE_CLASS_NAMES); \
		do \
			echo "val";\
			echo $$d;\
		    javah -classpath $(CLASS_PATH) -v -d $(JNI_PATH) $$d; \
		done
	
	for d in $(MAKE_SUBDIRS); \
		do \
		    make --directory=$$d; \
		done
	
	

clean: $(MAKE_SUBDIRS)
	for d in $(MAKE_SUBDIRS); \
		do \
		    make --directory=$$d clean; \
		done
	
	rm $$(find $(CLASS_PATH) -name "*.class")

