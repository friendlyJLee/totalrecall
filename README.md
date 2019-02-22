# Total Recall

This repository contains code for the following research.

## Publication

"**Total Recall: Persistence of Passwords in Android**," Jaeho Lee, Ang Chen and Dan S. Wallach, *The 26th Network and Distributed System Security Symposium (NDSS '19)*, San Diego, CA, USA, February 2019 [[PDF]](https://www.cs.rice.edu/~jl128/papers/ndss19-total_recall_jaeho_ang_dan.pdf) [[BibTex]](https://www.cs.rice.edu/~jl128/bibtex/ndss19.html)

### Motivation

In memory disclosure attacks, an unprivileged attacker can steal sensitive data from device memory. A good security practice for handling sensitive data, such as passwords, is to overwrite the data buffers with zeros once the data is no longer in use. This protects against attackers who gain a snapshot of a device’s physical memory. For instance, the recent memory dumping vulnerability in the Nexus 5X phone allows an attacker to obtain the full memory dump of the device even if the phone is locked.

e.g., [Google Nexus 5X Bootloader Unauthorized Memory Dumping via USB](https://alephsecurity.com/vulns/aleph-2016000)

<img src="https://user-images.githubusercontent.com/14894590/53221341-9522eb80-362e-11e9-9339-63ea19412128.png" width=300>

However, the password retention in memory is widespread in Android, and password strings are easily recognizable from memory dump. The lack of support in the Android framework and developers' various mistakes cause this retention problem.

<img src="https://user-images.githubusercontent.com/14894590/53221735-59892100-3630-11e9-8b87-5ae9d72371a5.png" height=55><img src="https://user-images.githubusercontent.com/14894590/53221757-74f42c00-3630-11e9-8ed5-d6b375100879.png" height=55>

### Solutions

We offer two solutions 
* **SecureTextView**: a secure version of Android TextView that can eliminate password retention in the Android framework.
* **KeyExporter**: a simple abstraction for accessing passwords that helps developers follows stronger cryptographic practices to prevent password misuse.


## Contents

* apps: list of analyzed password authentication apps 
* pmdump: process memory acquisition tool on Linux or Android
* keyexporter: Standalone KeyExporter APIs
* SecureTextView: Android framework patch for SecureTextView
* lockscreen_patch: Android framework patch for lockscreen process
* evaluation: apps after applying KeyExporter APIs

## People
* [Jaeho Lee](https://www.cs.rice.edu/~jl128)
* [Ang Chen](https://www.cs.rice.edu/~angchen)
* [Dan S. Wallach](https://www.cs.rice.edu/~dwallach)
