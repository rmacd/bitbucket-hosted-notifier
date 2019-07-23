package io.jenkins.plugins.rmacd.bitbucketNotification;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.*;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.CertificateCredentialsImpl;
import hudson.security.ACL;
import jenkins.model.Jenkins;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Base64;
import java.util.List;

public class NotificationUtils {

    private PrintStream logger;

    public int getURL(String strURL) throws NotificationCheckedException {
        HttpURLConnection conn = setupConnection(strURL);
        try {
            conn.setRequestMethod("GET");
            return conn.getResponseCode();
        } catch (IOException e) {
            throw new NotificationCheckedException("Unable to perform request: " + e.getMessage());
        }
        finally {
            if (null != conn) {
                conn.disconnect();
            }
        }
    }

    public int postJSON(String strURL, String body) throws NotificationCheckedException {
        HttpURLConnection conn = setupConnection(strURL);
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes());
            os.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            logger.println(content.toString());
            return conn.getResponseCode();
        } catch (ProtocolException e) {
            throw new NotificationCheckedException("Unable to POST data: " + e.getMessage());
        } catch (IOException e) {
            throw new NotificationCheckedException(e.getMessage(), e);
        }
    }

    private HttpURLConnection setupConnection(String strURL) throws NotificationCheckedException {
        try {
            String credentialsId = NotificationConfiguration.get().getCredentialsId();
            boolean hasCredentials = (null != credentialsId && !credentialsId.isEmpty());

            URL url = new URL(strURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            if (conn instanceof HttpsURLConnection) {
                SSLContext sc = null;
                // load client certificate, if applicable
                if (hasCredentials) {
                    List certificateCredentials = CredentialsProvider.lookupCredentials(StandardCertificateCredentials.class, Jenkins.get(), ACL.SYSTEM, new DomainRequirement());
                    for (Object credential : certificateCredentials) {
                        if (credential instanceof CertificateCredentials
                                && ((StandardCredentials) credential).getId().equalsIgnoreCase(credentialsId)
                        ) {
                            CertificateCredentials cert = (CertificateCredentialsImpl) credential;
                            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                            kmf.init(cert.getKeyStore(), cert.getPassword().getPlainText().toCharArray());
                            sc = SSLContext.getInstance("TLS");
                            sc.init(kmf.getKeyManagers(), null, null);
                        }
                    }
                }
                ((HttpsURLConnection) conn).setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String s, SSLSession sslSession) {
                        return true;
                    }
                });
                if (null == sc) sc = SSLContext.getDefault();
                ((HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
            }

            if (hasCredentials) {
                List usernamePasswordCredentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, Jenkins.get(), ACL.SYSTEM, new DomainRequirement());
                for (Object credential : usernamePasswordCredentials) {
                    if (credential instanceof UsernamePasswordCredentials
                            && ((StandardCredentials) credential).getId().equalsIgnoreCase(credentialsId)) {
                        UsernamePasswordCredentials entry = (UsernamePasswordCredentials) credential;
                        logger.println(
                            String.format("Got credentials: %s:%s", entry.getUsername(), entry.getPassword().getPlainText())
                        );
                        conn.setRequestProperty("Authorization", String.format(
                                "Basic %s", new String(Base64.getEncoder().encode(String.format("%s:%s", entry.getUsername(), entry.getPassword()).getBytes()))
                        ));
                    }
                }
            }
            conn.setRequestProperty("User-Agent", "Hosted Bitbucket Notification Plugin");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            return conn;

        } catch (MalformedURLException e) {
            throw new NotificationCheckedException("Invalid URL");
        } catch (IOException e) {
            throw new NotificationCheckedException(
                    String.format("Unable to connect to endpoint: %s, error was '%s'", strURL, e.getMessage())
            );
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException e) {
            throw new NotificationCheckedException(e.getMessage());
        }
    }

    public void setLogger(PrintStream logger) {
        this.logger = logger;
    }
}
