# Wendy

# Configure

```
WendyConfig.overrideProcessApiResponse() # After API calls are done, you can run your own code to process API success/error response.
WendyConfig.setErrorNotifier() # Get notified when important errors are thrown in your app. You should get errors and log them.
```

*Note: Wendy depends on [AndroidRealm](https://github.com/curiosityio/AndroidRealm) library to work with saving the models to a database. Since you are using this lib, I am assuming you are also using Realm since you are creating the models. So, make sure to configure AndroidRealm as well as this lib.*

### Where did the name come from?

User wants fast results. Doesn't want to wait for network calls --> Fast food. User doesn't want to wait for food. --> Wendy.