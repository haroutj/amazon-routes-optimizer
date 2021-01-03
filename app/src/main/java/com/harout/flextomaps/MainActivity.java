package com.harout.flextomaps;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    Bitmap bitmap;
    static ArrayList<String> addressBook;
    static ArrayList<Integer> numIndeces;
    EditText stt;
    double minLength;
    static boolean began;
    static int index;
    static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        bitmap = null;

        addressBook = new ArrayList<String>();
        numIndeces = new ArrayList<Integer>();
        began = false;
        index = 0;

        MainActivity.context = getApplicationContext();

        stt = findViewById(R.id.stt);

        final SharedPreferences settings = this.getSharedPreferences(
                "shared_prefs", Context.MODE_PRIVATE);

        stt.setText(settings.getString("stateCode", ""));

        TextWatcher sttTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                settings.edit().putString("stateCode", stt.getText().toString()).apply();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
        stt.addTextChangedListener(sttTextWatcher);

        findViewById(R.id.upload_screenshot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), 1);
            }
        });

        findViewById(R.id.btn_go).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean issueFound = false;

                if (!isValidStateAbbrev(stt.getText().toString())) {
                    stt.setTextColor(Color.rgb(176, 0, 32));
                    issueFound = true;
                }

                if (!((TextView) findViewById(R.id.upload_text)).getText().toString().equals("Uploaded successfully.")) {
                    ((TextView) findViewById(R.id.upload_text)).setTextColor(Color.rgb(176, 0, 32));
                    issueFound = true;
                }

                if (!issueFound) {
                    stt.setTextColor(Color.rgb(0, 0, 0));
                    ((TextView) findViewById(R.id.upload_text)).setTextColor(Color.rgb(0, 0, 0));

                    if (addressBook.size() == 0) {
                        ((TextView) findViewById(R.id.upload_text)).setText("No addresses were found in the uploaded image.");
                    } else {
                        final String[] addressesAsStrs = {""};
                        final TextView addressesText = findViewById(R.id.addresses);

                        addressesText.setText("");

                        new AsyncTask<Object, Object, Object>() {
                            @Override
                            protected Object doInBackground(Object... objects) {
                                optimizeAddressBook();

                                return null;
                            }

                            @Override
                            protected void onPreExecute() {
                                ((TextView) findViewById(R.id.num_order_text)).setText("");
                                ((ProgressBar) findViewById(R.id.progress_horizontal)).setProgress(0);
                                findViewById(R.id.progress_horizontal).setVisibility(View.VISIBLE);
                                findViewById(R.id.btn_go).setEnabled(false);

                                super.onPreExecute();
                            }

                            @Override
                            protected void onPostExecute(Object result) {
                                addressesText.setText(addressBook.size() + " addresses were found.");
                                LinearLayout mLlayout = findViewById(R.id.lLayout);

                                mLlayout.removeAllViewsInLayout();

                                for (String address : addressBook) {
                                    EditText myEditText = new EditText(context);
                                    myEditText.setTextSize(14f);
                                    myEditText.setBackground(null);
                                    myEditText.setPadding(0, 5, 0, 5);
                                    myEditText.setText(address);
                                    mLlayout.addView(myEditText);

                                    ((TextView) findViewById(R.id.num_order_text)).setText(numIndeces.toString().substring(1, numIndeces.toString().length() - 1));
                                }
                                findViewById(R.id.btn_savechanges).setVisibility(View.VISIBLE);
                                findViewById(R.id.progress_horizontal).setVisibility(View.GONE);
                                findViewById(R.id.btn_go).setVisibility(View.GONE);
                                findViewById(R.id.go_to).setVisibility(View.VISIBLE);
                            }
                        }.execute();
                    }
                }
            }
        });



        ((Button) findViewById(R.id.go_to)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                began = true;
                sendAddressesToMaps(MainActivity.this);
            }
        });

        if (!Settings.Secure.getString(this.getContentResolver(),"enabled_notification_listeners").contains(getApplicationContext().getPackageName()))
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));

        ((Button) findViewById(R.id.btn_savechanges)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addressBook.clear();
                LinearLayout lLayout = findViewById(R.id.lLayout);
                    for (int i = 0; i < lLayout.getChildCount(); i++) {
                        View v = lLayout.getChildAt(i);
                        if (v instanceof EditText) {
                            addressBook.add(((EditText)v).getText().toString());
                        }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            minLength = Double.MAX_VALUE;

            Uri selectedImage = data.getData();
            try {
                Bitmap newBitmap = base64ToBitmap(bitmapToBase64(ImageDecoder.decodeBitmap(ImageDecoder.createSource(this.getContentResolver(), selectedImage))));

                TextRecognizer textRecognizer = new TextRecognizer.Builder(MainActivity.this).build();

                if (textRecognizer.isOperational()) {
                    Frame imageFrame = new Frame.Builder().setBitmap(base64ToBitmap(bitmapToBase64(newBitmap))).build();
                    SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);

                    addressBook.addAll(findAddresses(textBlocks));
                } else {
                    ((TextView) findViewById(R.id.upload_text)).setText("Text recognizer not operational yet.");
                }

                if (bitmap != null) {
                    newBitmap = stitchBitmaps(bitmap, newBitmap);
                }

                bitmap = newBitmap;

                ((TextView) findViewById(R.id.upload_text)).setTextColor(Color.rgb(0, 0, 0));
                ((TextView)findViewById(R.id.upload_text)).setText("Uploaded successfully.");
                findViewById(R.id.imageView2).setVisibility(View.VISIBLE);
                ((ImageView)findViewById(R.id.imageView2)).setImageBitmap(bitmap);
                findViewById(R.id.btn_savechanges).setVisibility(View.GONE);
            } catch (IOException e) {
                ((TextView)findViewById(R.id.upload_text)).setText("Upload failed.");
            }
        }
    }

    public static Bitmap stitchBitmaps(Bitmap b1, Bitmap b2) {
        Bitmap stitchedBitmap = null;

        int width = Math.max(b1.getWidth(), b2.getWidth()), height = b1.getHeight() + b2.getHeight();

        stitchedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas comboImage = new Canvas(stitchedBitmap);

        comboImage.drawBitmap(b1, 0f, 0f, null);
        comboImage.drawBitmap(b2, 0f, b1.getHeight(), null);

        return stitchedBitmap;
    }

    private static String parseAddress(String raw) {
        String newStr = "";

        if (raw.indexOf("Apt") > 0 && raw.substring(raw.indexOf("Apt")).matches(".*\\d.*"))
            raw = raw.replaceFirst("Apt\\s*", "#");

        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) == '/') {
                newStr += "%2F";
            }
            else if (raw.charAt(i) == '#') {
                newStr += "%23";
            }
            else {
                newStr += raw.charAt(i);
            }
        }

        return newStr.trim().replaceAll("\\s+", "+");
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String b64) {
        byte[] imageAsBytes = Base64.decode(b64.getBytes(), Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
    }

    public ArrayList<String> findAddresses(SparseArray<TextBlock> textBlocks) {
        ArrayList<String> addressBook = new ArrayList<String>();

        Pattern pattern = Pattern.compile("\\d([A-Za-z]|\\s|\\d|[\\.\\,/\\-#])*[A-Z]+([A-Za-z]|\\s|\\d|[\\.\\,/\\-#])*[A-Z]{2,}([A-Za-z]|\\s|\\d|[\\.\\,/\\-#])*");
        Pattern area = Pattern.compile("[A-Za-zÀ-ÖØ-öø-ÿ]+");

        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));

            for (int j = 0; j < textBlock.getComponents().size(); j++) {
                boolean last = !(j < textBlock.getComponents().size() - 1);
                String text = textBlock.getComponents().get(j).getValue();
                Log.v("FlexToMaps", text);
                //((TextView) findViewById(R.id.upload_text)).setText(((TextView) findViewById(R.id.upload_text)).getText().toString() + "{s, " + matcher.start() + "}");
                Matcher matcher = pattern.matcher(text);
                Matcher matcher2 = null;

                String next = null;

                if (!last) {
                    next = textBlock.getComponents().get(j + 1).getValue();
                    matcher2 = area.matcher(next);
                }
                else if (i < textBlocks.size() - 1) {
                    next = textBlocks.get(textBlocks.keyAt(i + 1)).getComponents().get(0).getValue();
                    matcher2 = area.matcher(next);
                }

                if (matcher.find() && matcher2 != null && matcher2.find() && text.indexOf("#") != 0 && text.indexOf("%") == -1 && next != null && !Pattern.compile("[^A-Za-zÀ-ÖØ-öø-ÿ\\s]+").matcher(next).find()) {
                    addressBook.add(capitalizeFully(text.substring(matcher.start(), matcher.end())) + ", " + capitalizeFully(next) + ", " + stt.getText().toString().toUpperCase());
                }
                //((TextView) findViewById(R.id.upload_text)).setText(((TextView) findViewById(R.id.upload_text)).getText().toString() + "{e, " + matcher.end() + "}");
            }
        }
        Log.e("FlexToMap", addressBook.toString());
        return addressBook;
    }

    private static LatLng getLocationFromAddress(Context context, String strAddress) {

        Geocoder coder = new Geocoder(context);
        List<Address> address;
        LatLng p1 = null;

        try {
            // May throw an IOException
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null || address.size() == 0) {
                return null;
            }

            Address location = address.get(0);
            p1 = new LatLng(location.getLatitude(), location.getLongitude());

        } catch (IOException ex) {
        }

        return p1;
    }

    private double[][] matrix;
    public void populateMatrix() {
        matrix = new double[addressBook.size()][addressBook.size()];

        String websiteUrl = "https://dev.virtualearth.net/REST/v1/Routes/DistanceMatrix?origins=";
        for (String e : addressBook) {
            LatLng p1 = getLocationFromAddress(getApplicationContext(), e);
            websiteUrl +=  p1.latitude + "," + p1.longitude + ";";
        }

        if (websiteUrl.contains(";")) {
            websiteUrl = websiteUrl.substring(0, websiteUrl.length() - 1);
        }

        websiteUrl += "&destinations=";


        for (String e : addressBook) {
            LatLng p1 = getLocationFromAddress(getApplicationContext(), e);
            websiteUrl +=  p1.latitude + "," + p1.longitude + ";";
        }

        if (websiteUrl.contains(";")) {
            websiteUrl = websiteUrl.substring(0, websiteUrl.length() - 1);
        }

        websiteUrl += "&travelMode=driving&key=AuI3ynXYgyinHU_ud5ze6iP2NTM63pIcSFTYXNomqz4moIJKh0gAb7Wsmol2wGRI";

        String raw = "";
        try {
            raw = HttpGet(websiteUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<String> matches = new ArrayList<String>();
        Matcher m = Pattern.compile("\"destinationIndex\":\\d+\\.?\\d*,\"originIndex\":\\d+\\.?\\d*,\"totalWalkDuration\":\\d+\\.?\\d*,\"travelDistance\":\\d+\\.?\\d*,\"travelDuration\":\\d+\\.?\\d*")
                .matcher(raw);
        while (m.find()) {
            matches.add(m.group());
        }

        Log.e("FlexToMaps", matches.toString());

        for (String e : matches) {
            try {
                matrix[Integer.parseInt(getFromJson(e, "destinationIndex\":"))][Integer.parseInt(getFromJson(e, "originIndex\":"))] = Double.parseDouble(getFromJson(e, "travelDistance\":"));
            }
            catch (NumberFormatException exc) {
                exc.printStackTrace();
                Log.e("FlexToMaps", exc.toString());
            }
        }

        Log.e("FlexToMaps", Arrays.deepToString(matrix));
    }

    public String getFromJson(String string, String subString) {
        if (string.contains(subString)) {
            string = string.substring(string.indexOf(subString) + subString.length());

            if (string.contains(",")) {
                return string.substring(0, string.indexOf(","));
            }

            Log.e("FlexToMaps", string);

            return string;
        }

        return "";
    }

    private ArrayList<String> fullAddressBook;
    public double calcDrivingDistance(String addr1, String addr2) {
        int index = fullAddressBook.indexOf(addr1);
        int index2 = fullAddressBook.indexOf(addr2);

        return matrix[index][index2];
    }

    public void optimizeAddressBook() {
        populateMatrix();

        for (int i = 0; i < addressBook.size(); i++) {
            if (getLocationFromAddress(getApplicationContext(), addressBook.get(i)) == null) {
                addressBook.remove(i);
                i--;
            }
        }

        numIndeces.clear();

        if (addressBook.size() > 0) {
            HashMap<String, Integer> numOrderMap = new HashMap<String, Integer>();

            int num = 1;
            for (String e : addressBook) {
                numOrderMap.put(e, num);
                num++;
            }

//            String startLocation = ((EditText)findViewById(R.id.start_location)).getText().toString();
//
//            double closest = calcDrivingDistance(startLocation, addressBook.get(0));
//            String closestAddr = addressBook.get(0);
//            int iAt = 0;
//
//            for (int i = 1; i < addressBook.size(); i++) {
//                double linearDist = calcDrivingDistance(homeAddress, addressBook.get(i));
//                if (linearDist < closest) {
//                    closest = linearDist;
//                    closestAddr = addressBook.get(i);
//                    iAt = i;
//                }
//            }

            ((ProgressBar)findViewById(R.id.progress_horizontal)).setMax(addressBook.size());

            ArrayList<String> newBook = new ArrayList<String>();
            fullAddressBook = new ArrayList<String>(addressBook);

            newBook.add(addressBook.get(0));
            numIndeces.add(numOrderMap.get(addressBook.get(0)));
            addressBook.remove(0);

            while (addressBook.size() > 0) {
                double shortest = calcDrivingDistance(newBook.get(newBook.size() - 1), addressBook.get(0));
                String shortestAddr = addressBook.get(0);
                int jAt = 0;
                for (int j = 0; j < addressBook.size(); j++) {
                    double linearDist = calcDrivingDistance(newBook.get(newBook.size() - 1), addressBook.get(j));
                    if (linearDist < shortest) {
                        shortest = linearDist;
                        shortestAddr = addressBook.get(j);
                        jAt = j;
                    }
                }

                newBook.add(shortestAddr);
                numIndeces.add(numOrderMap.get(shortestAddr));
                addressBook.remove(jAt);

                ((ProgressBar)findViewById(R.id.progress_horizontal)).setProgress(newBook.size());
            }

            addressBook = newBook;
        }
        else {
            ((TextView) findViewById(R.id.upload_text)).setText("No valid addresses were detected in the uploaded image.");
        }
    }

    public static void sendAddressesToMaps(Context context) {
        String address;
        if (index == 0) {
            address = "https://www.google.com/maps/dir//";
        }
        else {
            address = "https://www.google.com/maps/dir/" + parseAddress(addressBook.get(index-1)) + "/";
        }
        int i = 0;
        while (index < addressBook.size() && i < 10) {
            address += parseAddress(addressBook.get(index)) + "/";

            index++;
            i++;
        }

        if (index >= addressBook.size()) {
            began = false;
            index = 0;
        }

        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(address));
        Log.v("FlexToMaps", address);

        intent.setPackage("com.google.android.apps.maps");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static boolean isValidStateAbbrev(String state) {
        String states = "|AL|AK|AS|AZ|AR|CA|CO|CT|DE|DC|FM|FL|GA|GU|HI|ID|IL|IN|IA|KS|KY|LA|ME|MH|MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|MP|OH|OK|OR|PW|PA|PR|RI|SC|SD|TN|TX|UT|VT|VI|VA|WA|WV|WI|WY|";
        return state.length() == 2 && states.indexOf(state) >  0 && !state.contains("|");
    }

    public static String capitalizeFully(String str) {
        str = str.toLowerCase();

        String[] strArray = str.split("\\s");

        StringBuilder builder = new StringBuilder();
        for (String s : strArray) {
            String cap = s.substring(0, 1).toUpperCase() + s.substring(1);
            builder.append(cap + " ");
        }

        return builder.toString().trim();
    }

    public static String HttpGet(String rawUrl) throws IOException {
        StringBuilder result = new StringBuilder();
        URL url = new URL(rawUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        return result.toString();
    }
}
