FLAGS=-Wall -fpic -Werror
LIBS=-lpthread -lrt
CURRENT_DIR = $(shell pwd)

MAIN_NAME=main
DYN_LIB_NAME=libdatagramReceiver.so
STA_LIB_NAME=datagramReceiver.a

all : main dynlib statlib

DatagramSocket.o : DatagramSocket.c
	gcc $(FLAGS) -c DatagramSocket.c

CharArray.o : CharArray.c
	gcc $(FLAGS) -c CharArray.c

ErrorHandler.o : ErrorHandler.c
	gcc $(FLAGS) -c ErrorHandler.c

LinkedList.o : LinkedList.c
	gcc $(FLAGS) -c LinkedList.c

main.o : main.c
	gcc $(FLAGS) -c main.c 

main : LinkedList.o CharArray.o ErrorHandler.o DatagramSocket.o main.o
	gcc $(FLAGS) -o $(MAIN_NAME) DatagramSocket.o CharArray.o ErrorHandler.o LinkedList.o main.o $(LIBS)

dynlib : LinkedList.o CharArray.o ErrorHandler.o DatagramSocket.o
	gcc $(FLAGS) -shared -o $(DYN_LIB_NAME) LinkedList.o CharArray.o ErrorHandler.o DatagramSocket.o

statlib : LinkedList.o CharArray.o ErrorHandler.o DatagramSocket.o
	ar rcs $(STA_LIB_NAME) LinkedList.o CharArray.o ErrorHandler.o DatagramSocket.o

clean :
	rm *.o $(MAIN_NAME) $(DYN_LIB_NAME) $(STA_LIB_NAME)
