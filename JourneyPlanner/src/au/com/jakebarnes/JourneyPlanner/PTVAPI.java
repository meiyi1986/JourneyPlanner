package au.com.jakebarnes.JourneyPlanner;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class PTVAPI {
    /**
     * Generates a signature using the HMAC-SHA1 algorithm 
     * 
     * @param privateKey - Developer Key supplied by PTV
     * @param uri - request uri (Example :/v2/HealthCheck) 
     * @param developerId - Developer ID supplied by PTV
     * @return Unique Signature Value  
     */
    public static String generateSignature(final String privateKey, final String uri, final int developerId)
    {
        String encoding = "UTF-8";
        String HMAC_SHA1_ALGORITHM = "HmacSHA1";
        String signature;
        StringBuffer uriWithDeveloperID = new StringBuffer();
        uriWithDeveloperID.append(uri).append(uri.contains("?") ? "&" : "?").append("devid="+developerId);     
        try
        {
            byte[] keyBytes = privateKey.getBytes(encoding);
            byte[] uriBytes = uriWithDeveloperID.toString().getBytes(encoding);
            Key signingKey = new SecretKeySpec(keyBytes, HMAC_SHA1_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);
            byte[] signatureBytes = mac.doFinal(uriBytes);
            StringBuffer buf = new StringBuffer(signatureBytes.length * 2);
            for (byte signatureByte : signatureBytes)
            {
                int intVal = signatureByte & 0xff;
                if (intVal < 0x10)
                {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(intVal));
            }
            signature = buf.toString();
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }
        catch (InvalidKeyException e)
        {
            throw new RuntimeException(e);
        }
        return signature.toString().toUpperCase();
    }
    
    /**
     * Generate full URL using generateSignature() method
     * 
     * @param privateKey - Developer Key supplied by PTV (Example :  "92dknhh31-6a30-4cac-8d8b-8a1970834799");
     * @param uri - request uri (Example :"/v2/mode/2/line/787/stops-for-line) 
     * @param developerId - Developer ID supplied by PTV( int developerId )
     * @return - Full URL with Signature
     */
    public static String generateCompleteURLWithSignature(final String privateKey, final String uri, final int developerId)
    {
        
        String baseURL="http://timetableapi.ptv.vic.gov.au";
        StringBuffer url = new StringBuffer(baseURL).append(uri).append(uri.contains("?") ? "&" : "?").append("devid="+developerId).append("&signature="+generateSignature(privateKey, uri, developerId));
//        System.out.println(url.toString());
        return url.toString();
    }
}
