#include "syscall.h"
#include "stdlib.h"
int main() {
   int fdA = creat("unlinkTestFileA.txt");
   int fdB = creat("unlinkTestFileB.txt");
   printf("fdA: %d; Supposed to be 2;\n", fdA);
   printf("fdB: %d; Supposed to be 3;\n", fdB);


   int status = unlink("unlinkTestFileA.txt");     // Normal Unlink
   printf("unlinkStatus: %d; Supposed to be 0;\n", status);

   int fdC = open("USConst.txt");                  // Check the fd has not
                                                   // yet be freed
   printf("fdC: %d; Supposed to be 4;\n", fdC);


   status = close(2);         // Close the doomed file
   printf("closeStatus: %d; Supposed to be 0;\n", status);

   status = unlink("unlinkTestFileA.txt");   // Check no double unlink
   printf("unlinkStatus: %d; Supposed to be -1;\n", status);
   status = unlink("nonExistent");           // Check no unlink non-exist
   printf("unlinkStatus: %d; Supposed to be -1;\n", status);

   fdA = open("0Char.txt");   // Check if free fd is reused
   printf("fdA: %d; Supposed to be 2;\n", fdA);
   status = close(3);                        // Close the file first
   printf("closeStatus: %d; Supposed to be 0;\n", status);
   status = unlink("unlinkTestFileB.txt");   // Then unlink
   printf("unlinkStatus: %d; Supposed to be 0;\n", status);

   halt();
   return 0;
}
