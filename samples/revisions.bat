@echo off
rem *** Update copyright and email headers ***

for /R %%f in (*.bat) do (
echo %%f
copy %%f temp.sed >> log.sed
sed "s/commons-logging-1\.1\.jar/commons-logging-1\.1\.1\.jar/" temp.sed > %%f
del temp.sed
)
del log.sed
