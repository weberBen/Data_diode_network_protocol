FLAGS=-fpic -Wall -Werror

PATH_LIB=../datagramSocket_C

LIBS=-I/usr/lib/jvm/java-8-openjdk-amd64/include -I/usr/lib/jvm/java-8-openjdk-amd64/include/linux -I$(PATH_LIB) $(PATH_LIB)/datagramReceiver.a

NAME=libmaLib.so

all : lib

ReceiveTools_UdpSocket_Pointer.o : ReceiveTools_UdpSocket_Pointer.c
	gcc $(FLAGS) -c ReceiveTools_UdpSocket_Pointer.c $(LIBS)

lib : ReceiveTools_UdpSocket_Pointer.o
	gcc $(FLAGS) -shared -o $(NAME) ReceiveTools_UdpSocket_Pointer.o $(LIBS)

clean :
	rm *.o $(NAME)
