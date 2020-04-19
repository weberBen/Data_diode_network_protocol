#ifndef H_CHAR_ARRAY
#define H_CHAR_ARRAY

typedef struct _charArray
{
    char * array;
    unsigned int length;
} CharArray;


/********************************************************************
 *                      INIT FUNCTIONS
 * 
 * 
 * ******************************************************************
*/

CharArray * createAndInitCharArray(char * buffer, unsigned int buffer_size);
CharArray * createCharArray(unsigned int buffer_size);
CharArray * createEmptyCharArray();
void freeCharArray(CharArray * char_array);


#endif
