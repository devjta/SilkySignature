
SilkySignature
========

A smooth and silky signature pad for android.

![app screenshot](signature.png)

Updates
--------
Method added
* ` getTransparentSignatureBitmapTrimOnStrokes()`  - returns a transparent signature bitmap, but the triming process is way faster.
* ` insertStaticText(String text)`  - adds a text at the lower end of the sign-pad (text-attributes cannot be modified).
* ` addStaticText(String text)(String text)`  - adds a text at the lower end of the sign-pad and returns the TextBuilder, where you can changed text-attributes. You have to run "build" to add the text.
* ` setHintText(String/int text)`  - adds a hint-text at the middle of the sign-pad, which dissappears as soon as someone signs (will be automaticaly shown when it is cleared again).
* ` setHintTextColor(int color)`  - sets the color of the hint-text.
* ` setHintTextColorRes(int color)`  - sets the color of the hint-text.
* ` setHintBorderColor(int color)`  - sets the border-color of the hint-text.
* ` setHintBorderColorRes(int color)`  - sets the border-color of the hint-text.




Bug fixed
* SVG double click bug fixed

Download
--------
Download forked version
```
https://jitpack.io/#devjta/SilkySignature
```

Download SilkySignature or grab via Gradle:


```groovy
implementation 'com.github.devjta:SilkySignature:a3506fe4f3'
```
or via Maven
```xml
<dependency>
<groupId>com.github.devjta</groupId>
<artifactId>SilkySignature</artifactId>
<version>a3506fe4f3</version>
<type>aar</type>
</dependency>
```

Note
--------
You MUST request runtime permission when you are trying to save the signature image on version 6.0  or higher!


Example
--------

Add this to layout file
```xml

<com.williamww.silkysignature.views.SignaturePad
xmlns:android="http://schemas.android.com/apk/res/android"
xmlns:app="http://schemas.android.com/apk/res-auto"
android:id="@+id/signature_pad"
android:layout_width="match_parent"
android:layout_height="match_parent"
app:penColor="@android:color/black"
/>
```

Control in code
```java

mSignaturePad = (SignaturePad) findViewById(R.id.signature_pad);
mSignaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {
@Override
public void onSigned() {
//Event triggered when the pad is signed
}

@Override
public void onClear() {
//Event triggered when the pad is cleared
}
});
```
To get signature image
* `getTransparentSignatureBitmapTrimOnStrokes()` - A signature bitmap with a transparent background which trims the bitmap very fast.
* `getSignatureBitmap()` - A signature bitmap with a white background.
* `getTransparentSignatureBitmap()` - A signature bitmap with a transparent background.
* `getSignatureSvg()` - A signature Scalable Vector Graphics document.
