package com.github.kiulian.downloader.base64;

public class Base64EncoderImpl implements Base64Encoder {

    private static final char[] chars = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
        'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
        'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
        'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', '-', '_'
    };

    @Override
    public String encodeToString(byte[] bytes) {
        // complete 6bit chars (8bit bytes, 6bit chars)
        int charCount = (bytes.length * 8) / 6;
        
        // complete 6bit remaining chars (1 or 2)
        // same as remaining bytes
        int remainingCharCount = charCount % 4;
        charCount -= remainingCharCount;
        
        StringBuilder builder = new StringBuilder();
        
        int bi = 0; // byte index
        int ci = 0; // char index
        
        // complete 6bit chars
        while (ci < charCount) {
            // 24bit integer made of the next 3 bytes 
            int val = (bytes[bi++] & 0xff) << 16
                    | (bytes[bi++] & 0xff) << 8
                    | bytes[bi++] & 0xff;
            
            // 24 bits : 4x6bit chars
            for (int i = 3; i >= 0; i--) {
                builder.append(chars[(val >>> (i * 6)) & 63]);
            }
            ci += 4;
        }
        
        // remaining bytes
        if (remainingCharCount > 0) {
            if (remainingCharCount == 1) {
                // 1 byte, 8 bits : 1x6bit char + 1x2bit char
                int val = bytes[bi] & 0xff;
                // first 6 bits/8
                builder.append(chars[val >> 2]);
                // last 2 bits/8, 4 right padding
                builder.append(chars[(val << 4) & 63]);
                builder.append("==");
            } else {
                // 2 bytes, 16 bits: 2x6bit chars + 1x4bit char
                // 16bit integer made of last 2 bytes
                int val = (bytes[bi++] & 0xff) << 8
                        | (bytes[bi] & 0xff);
                // first 6 bits/16 (0-5)
                builder.append(chars[(val >>> 10) & 63]);
                // middle 6 bits/16 (6-11)
                builder.append(chars[(val >>> 4) & 63]);
                // last 4 bits/16 (12-15), 2 right padding
                builder.append(chars[(val << 2) & 63]);
                builder.append('=');
            }
        }
        return builder.toString();
    }
}
