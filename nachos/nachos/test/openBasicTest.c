#include "syscall.h"
int main () {
   int fdA = open("testFileA");
   printf("fdA: %d; Supposed to be 2\n", fdA);
   int fdA2 = open("testFileA");
   printf("fdA2: %d; Supposed to be 3\n", fdA2);
   int fdB = open("testFileB");
   printf("fdB: %d; Supposed to be 4\n", fdB);
   int fdNE = open("nonExistent");
   printf("fdNE: %d; Supposed to be -1\n", fdNE);

   halt();
   return 0;
}

