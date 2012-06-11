#!/bin/bash
if [ $# == 0 ]; then
   echo Usage: commit.sh \"commit message\"
   exit 1
fi

svn add `svn status | grep -e ^? | awk '{print $2}'`
svn delete `svn status | grep -e ^! | awk '{print $2}'`
svn status
read
svn commit -m "$*"