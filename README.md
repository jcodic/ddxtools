# ddxtools
Set of very simple command line tools:<br/>
1. Crypt - encrypt file/path or string<br/>
   <sub>example:  crypt encrypt c:\temp output.enc inc=.\*jpg exc=.\*\\\avatars\\\\.\*</sub><br/>
2. Hash - calculate hash of file/path or string<br/>
   <sub>example: hash hash_string "end of file" algo=md5 toclip</sub><br/>
   <sub>example: hash hash c:\temp hash=c:\temp\my.hash exc=.\*my.hash$ algo=sha-256</sub><br/>
3. Hashcrack - crack hash by dictionary/combination or brute force<br/>
   <sub>example: hashcrack crack 0827b22b142ebb1b18d9d93f19f770c6 algo=md5 "mask=?l?l?l ?l?l file" status=10</sub><br/>
   <sub>example: hashcrack crack 0827b22b142ebb1b18d9d93f19f770c6 algo=md5 0d=c:\dict\example.dict "mask=?0 ?0 ?0"</sub><br/>
4. Performance - evaluate performance on compress/encrypt using specified number of threads<br/>
   <sub>example: performance run time=30</sub><br/>
   <sub>example: performance run time=30 threads=8 fill=false compress=false encrypt=false</sub><br/>
5. Matches - find same files on different paths & disks, comparing them by name,size or content<br/>
   <sub>example: matches path=q:</sub><br/>
   <sub>example: matches path=q: path=u:\temp name=false content=true export=c:\report.csv</sub><br/>
6. Fill - write file filled with random bytes including hash code for later checking<br/>
   <sub>example: fill fill data.raw 100gb algo=sha-512 buffer=256kb</sub><br/>
   <sub>example: fill check data.raw</sub><br/>
7. FileTools - set of tools to work with binary file<br/>
   <sub>example: filetools search binfile.dat FFFCBB</sub><br/>
   <sub>example: filetools cut binfile.dat pieces 100kb max=10</sub><br/>
   <sub>example: filetools inverse binfile.dat inversed.dat shuffle=10000 filebf=100kb</sub><br/>
   <sub>example: filetools entropy binfile.dat</sub><br/>
   <sub>example: filetools print binfile.dat start=1000 len=20</sub><br/>
<br/>
<sub>
Every tool has it's running file for linux/windows (sh/bat). Running with no parameters prints help on commands.<br/>
All tools contained in one compiled file (ddx-crypto-ver.jar) which you can find in 'target' directory with no dependecies.<br/>
Put compiled file in some directory on disk and fix script files lines -cp "path_to_your_dir\*"<br/>
Add script files in path, so you can call it simple, from command line, anywhere.<br/>
</sub>
