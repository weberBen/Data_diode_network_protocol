FLAGS=-fpic -Wall -Werror -std=c99

LIBS=-I/usr/lib/jvm/java-8-openjdk-amd64/include -I/usr/lib/jvm/java-8-openjdk-amd64/include/linux 

NAME=libmaLib.so

all : lib

GeneralTools_FilePreallocator.o : GeneralTools_FilePreallocator.c
	gcc $(FLAGS) -c GeneralTools_FilePreallocator.c $(LIBS)

lib : GeneralTools_FilePreallocator.o
	gcc $(FLAGS) -shared -o $(NAME) GeneralTools_FilePreallocator.o $(LIBS)

clean :
	rm *.o $(NAME)
