#!/bin/bash
git submodule update --init
cd ChatHub-Shared/
mvn install
cd ..
mvn install