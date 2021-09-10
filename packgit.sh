#!/bin/bash

set -e

tar -cf git.tar .git
rm -rf .git
