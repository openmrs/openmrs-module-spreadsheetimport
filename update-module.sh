#!/bin/bash
if ! [ $# == 2 ]; then
   echo Usage: update-module admin-password module-file
   exit 1
fi
if ! [ -f $2 ]; then
   echo Error: module file $2 does not exist
   exit 1
fi
curl -c /tmp/cookie.txt -d uname=admin -d pw=${1} http://localhost:8080/openmrs/loginServlet
curl -b /tmp/cookie.txt -F action=upload -F update=true -F moduleFile=\@$2 http://localhost:8080/openmrs/admin/modules/module.list
rm -rf /tmp/cookie.txt > /dev/null 2>&1
