#include <stdlib.h>
#include <stdio.h>

#include "CharArray.h"


CharArray * createAndInitCharArray(char * buffer, unsigned int buffer_size)
{
    CharArray * char_array = (CharArray *)malloc(sizeof(CharArray));
    if(char_array==NULL)
        return NULL;

    char_array->array = buffer;
    char_array->length = buffer_size;

    return char_array;
}

CharArray * createCharArray(unsigned int buffer_size)
{
    char * buffer = (char *)malloc(sizeof(char)*buffer_size);
    if(buffer==NULL)
        return NULL;

    return createAndInitCharArray(buffer, buffer_size);
}

CharArray * createEmptyCharArray()
{
    return createAndInitCharArray(NULL, 0);
}

void freeCharArray(CharArray * char_array)
{
    if(char_array!=NULL)
    {
        free(char_array->array);
        free(char_array);
    }
}