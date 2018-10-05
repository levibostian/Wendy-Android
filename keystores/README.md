Put keystore files in here.

If you have not created a keystore file yet, run command keytool -genkey -v -keystore KEYSTORE-NAME.keystore -alias KEYSTORE-ALIAS -keyalg RSA -validity 10000 -storepass PASSWORD1 -keypass PASSWORD2 (replacing KEYSTORE-NAME to name of your app, KEYSTORE-ALIAS to name of your app, PASSWORD1 to a password used for your keypass password and PASSWORD2 to another password for your keypass password) NOTE: do not lose the file that this command creates. You will not be able to generate another one!

