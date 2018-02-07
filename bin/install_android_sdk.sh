#!/bin/bash

# Download and unzip the Android SDK tools (if not already there thanks to the cache mechanism)
# Latest version available here: https://developer.android.com/studio/index.html#downloads
if test ! -e $HOME/android-sdk-dl/sdk-tools.zip ; then curl https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip > $HOME/android-sdk-dl/sdk-tools.zip ; fi
unzip -qq -n $HOME/android-sdk-dl/sdk-tools.zip -d $HOME/android-sdk
# Install or update Android SDK components (will not do anything if already up to date thanks to the cache mechanism)
echo y | $HOME/android-sdk/tools/bin/sdkmanager 'tools' > /dev/null
echo y | $HOME/android-sdk/tools/bin/sdkmanager 'platform-tools' > /dev/null
echo y | $HOME/android-sdk/tools/bin/sdkmanager 'build-tools;27.0.3' > /dev/null
echo y | $HOME/android-sdk/tools/bin/sdkmanager 'platforms;android-27' > /dev/null
echo y | $HOME/android-sdk/tools/bin/sdkmanager 'extras;google;m2repository' > /dev/null
