# ddxtools
Set of very simple command line tools:<br/>
1. Crypt - encrypt file/path or string<br/>
   example:  crypt encrypt c:\temp output.enc inc=.\*jpg exc=.\*\\\avatars\\\\.\*<br/>
2. Hash - calculate hash of file/path or string<br/>
   example: hash hash_string "end of file" algo=md5 toclip<br/>
   example: hash hash c:\temp hash=c:\temp\my.hash exc=.\*my.hash$ algo=sha-256
3. Hashcrack - crack hash by dictionary/combination or brute force<br/>
   example: hashcrack crack 0827b22b142ebb1b18d9d93f19f770c6 algo=md5 "mask=?l?l?l ?l?l file" status=10<br/>
   example: hashcrack crack 0827b22b142ebb1b18d9d93f19f770c6 algo=md5 0d=c:\dict\example.dict "mask=?0 ?0 ?0"<br/>
4. Performance - evaluate performance on compress/encrypt using specified number of threads<br/>
   example: performance run time=30<br/>
   example: performance run time=30 threads=8 fill=false compress=false encrypt=false<br/>
<br/>
Every tool has it's running file for linux/windows. Running with no parameters prints help on commands.<br/>
