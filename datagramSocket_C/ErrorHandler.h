#ifndef H_ERROR_HANDLER
#define H_ERROR_HANDLER


typedef struct s_error
{
    char * msg;
} _error;

typedef _error * Error;


void removeError(Error * error);
int setError(Error * error, char * msg);
void initError(Error * error);
char * getErrorMsg(Error * error);
int isDefaultError(Error * error);

#define OUT_OF_MEMORY "Impossible d'allouer de la mémoire"


#endif