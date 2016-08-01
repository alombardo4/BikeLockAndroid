# BikeLockAndroid
This app controls a BLE bike lock developed for CS 4605, Mobile and Ubiquitous Computing.

# Functionality
<li>Pair with multiple locks</li>
<li>Nickname locks</li>
<li>Edit lock passwords</li>
<li>Unlock from phone and watch</li>

# Setup
<li>Requires Android Studio</li>
<li>Supports Android 4.3+</li>
<li>Wear app requires Android Wear 5.0+</li>

# Details
The connection is verified using the following method:
<ol>
<li>App sends "unlock" command to lock</li>
<li>2. Lock sends challenge value to app</li>
<li>App appends stored password to challenge value and performs SHA-256 hash and sends to lock</li>
<li>Lock generates its own SHA-256 hash and validates app's hash</li>
<li>If hashes match, lock triggers solenoids.</li>
</ol>
