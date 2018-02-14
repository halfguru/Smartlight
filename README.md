# Smartlight

The cloud side is composed of four Amazon Web Services (AWS) services. Those are Cognito, IAM, S3, and Lambda. AWS Cognito handles user sign in and authentication through the phone applications. It offers the option to create accounts, or even authenticate users using existing social media accounts. AWS IAM (Identity and Access Management) is necessary for user authentication and authorization in order for the applications to have the appropriate permissions to access the resources they need and run. Cognito runs on top of IAM to facilitate user authentication, such as by adding social media login options. AWS S3 (Simple Storage Service) is used to store the data, in the form of files. Last but not least, AWS Lambda handles computations. It runs code on the cloud when necessary, and does not require a specific computer or virtual machine to be rented, thus being most cost efficient.

### Installing

Install Android Studio and build/compile 

## Built With

* [Android Studio](https://developer.android.com/studio/index.html) - The Official IDE for Android
* [Maven](https://maven.apache.org/) - Dependency Management

## Contributing

Please read [CONTRIBUTING.md](https://gist.github.com/PurpleBooth/b24679402957c63ec426) for details on our code of conduct, and the process for submitting pull requests to us.

## Authors

* Simon Ho
* Mete Aykul
* Anastasios Alexandridis
* Junchao Wang
## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* Professor Zeljko Zilic who supervised this project for ECSE 682
