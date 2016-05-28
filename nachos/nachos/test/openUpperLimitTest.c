#include "syscall.h"
int main () {
   int i;
   for (i = 2; i <= 20; i++) {
      int fdA = open("testFileA");
      if (i < 16) {
         printf("fdA: %d; Supposed to be %d\n", fdA, i);
      } else {
         printf("fdA: %d; Upper Limit Hit, thus supposed to be -1\n", fdA);
      }
   }

   halt();
   return 0;
}

