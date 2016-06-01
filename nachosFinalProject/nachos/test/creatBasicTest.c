#include "syscall.h"
int main () {

   int fdNE = open("nonExistent");
   printf("fdNE: %d; Supposed to be -1\n", fdNE);
   fdNE = open("nonExistent");
   printf("fdNE: %d; Supposed to be -1\n", fdNE);

   fdNE = creat("nonExistent");
   printf("fdNE: %d; Supposed to be 2\n", fdNE);
   fdNE = creat("nonExistent");
   printf("fdNE: %d; Supposed to be 3\n", fdNE);

   fdNE = open("nonExistent");
   printf("fdNE: %d; Supposed to be 4\n", fdNE);
   fdNE = creat("nonExistent");
   printf("fdNE: %d; Supposed to be 5\n", fdNE);

   fdNE = open("nonExistent2");
   printf("fdNE: %d; Supposed to be -1\n", fdNE);
   fdNE = creat("nonExistent2");
   printf("fdNE: %d; Supposed to be 6\n", fdNE);

   halt();
   return 0;
}

