package com.mogobiz.google.client

import akka.dispatch.Futures
import com.mogobiz.common.client.Client
import com.mogobiz.common.client.ClientConfig
import com.mogobiz.common.client.Request
import com.mogobiz.common.rivers.spi.RiverConfig
import com.mogobiz.http.client.HTTPClient
import com.mogobiz.http.client.header.HttpHeaders
import com.mogobiz.common.client.BulkAction
import com.mogobiz.common.client.BulkItem
import com.mogobiz.common.client.BulkResponse
import com.mogobiz.common.client.Item
import com.mogobiz.common.client.SearchResponse
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder
import org.apache.commons.codec.binary.Base64
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.Callable

/**
 *
 * Created by stephane.manciot@ebiznext.com on 06/05/2014.
 */
@Slf4j
final class GoogleClient implements Client{

    private static GoogleClient instance

    private static final HTTPClient client = HTTPClient.getInstance()
    public static final String CONTENT_API_SERVICE = 'https://www.googleapis.com/auth/content'
    public static final String APP = 'mogobiz'//'ebiznext-mogobiz-0.1'

    private GoogleClient(){}

    static GoogleClient getInstance(){
        if(instance == null){
            instance = new GoogleClient()
        }
        instance
    }

    @Override
    Future<BulkResponse> bulk(
            final RiverConfig config,
            final List<BulkItem> items,
            ExecutionContext ec){
        def clientConfig = config.clientConfig
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        def version = clientConfig.config?.version as Integer
        if(!version){
            version = 2
        }
        if(version == 2){
            xml.'batch'(){
                items?.each { item ->
                    'entry'(batch_id:(item.id as String), method:item.action.name().toLowerCase()){
                        merchant_id(clientConfig.merchant_id)
                        switch(item.action){
                            case BulkAction.DELETE:
                                product_id(item.id)
                                break
                            default:
                                product(){
                                    id(item.id)//FIXME
                                    item.map?.each {k, v ->
                                        switch(k){
                                            case 'imported':
                                                break
                                            case 'id':
                                                offer_id(v)
                                                break
                                            case 'content':
                                                description(v)
                                                break
                                            case ['price', 'sale_price', 'shipping_price']:
                                                "${k}"(currency:config.currencyCode, v)
                                                break
                                            default:
                                                if(v instanceof Map){
                                                    "${k}"{
                                                        (v as Map).each {k2,v2 ->
                                                            if(v2.toString().trim().length() > 0){
                                                                "${k2}"(v2)
                                                            }
                                                        }
                                                    }
                                                }
                                                else if(v.toString().trim().length() > 0){
                                                    "${k}"(v)
                                                }
                                                break
                                        }
                                    }
                                }
                                break
                        }
                    }
                }
            }
        }
        else{
            xml.'feed'(xmlns: 'http://www.w3.org/2005/Atom', 'xmlns:batch': 'http://schemas.google.com/gdata/batch'){
                items?.each { item ->
                    entry(xmlns: 'http://www.w3.org/2005/Atom', 'xmlns:sc': 'http://schemas.google.com/structuredcontent/2009'){
                        'batch:operation'(type:item.action.name().toUpperCase())
                        'batch:id'(item.id)
                        item.map?.each {k, v ->
                            switch(k){
                                case 'imported':
                                    break
                                case 'id':
                                    'sc:id'(v)
                                    break
                                case 'title':
                                    title(v)
                                    break
                                case 'content':
                                    content(type:'text', v)
                                    break
                                case 'link':
                                    link(rel:'alternate', type:'text/html', href:v)
                                    break
                                case ['price', 'sale_price', 'shipping_price']:
                                    'sc:attribute'(name:k, unit:config.currencyCode, v)
                                    break
                                default:
                                    if(v instanceof Map){
                                        'sc:group'(name:k.trim().split('_')?.join(' ')){
                                            (v as Map).each {k2,v2 ->
                                                if(v2.toString().trim().length() > 0){
                                                    'sc:attribute'(name:k2.trim().split('_')?.join(' '), v2)
                                                }
                                            }
                                        }
                                    }
                                    else if(v.toString().trim().length() > 0){
                                        'sc:attribute'(name:k.trim().split('_')?.join(' '), v)
                                    }
                            }
                        }
                    }
                }
            }
        }
        final String body = writer.toString()
        Futures.future(new Callable<BulkResponse>() {
            @Override
            BulkResponse call() throws Exception {
                def conn = null
                try{
                    HttpHeaders headers = new HttpHeaders()
                    String token =  requestAccessToken(clientConfig)
                    String authz = 'Bearer ' + token
                    headers.setHeader('Authorization', authz)
                    headers.setHeader('Content-Type', version == 2 ? 'application/xml' : 'application/atom+xml')
                    boolean debug = config.clientConfig.debug
                    String url = 'https://www.googleapis.com/content/v2/products/batch?dryRun=' + config.dry_run
                    if(version != 2){
                        url = new StringBuffer('https://content.googleapis.com/content/v2/')
                                .append(config.clientConfig.merchant_id)
                                .append('/items/products/generic/batch?')
                                .append(config.dry_run?'dry-run':'')
                                .toString()
                    }
                    conn = client.doPost(
                            [debug:debug],
                            url,
                            [:],
                            body,
                            headers)
                    int responseCode = conn.responseCode
                    if(responseCode == 200){
                        // TODO
                        GPathResult result = client.parseTextAsXML([debug:debug], conn)
                                .declareNamespace(
                                sc: 'http://schemas.google.com/structuredcontent/2009',
                                batch: 'http://schemas.google.com/gdata/batch'
                        )
                        def _items = []
                        result.'*'.findAll{it.name() == 'entry'}.each {entry ->
                            _items << parseEntry(entry as GPathResult)
                        }
                        new BulkResponse(items: _items)
                    }
                    else{
                        client.getText([debug: debug], conn)
                        new BulkResponse(items: [])
                    }
                }
                finally{
                    client.closeConnection(conn)
                }
            }
        }, ec)
    }

    @Override
    SearchResponse search(Request request){
        SearchResponse response = null
        def clientConfig = request.clientConfig
        boolean debug = clientConfig.debug
        StringBuffer buffer = new StringBuffer('https://content.googleapis.com/content/v2/')
                .append(clientConfig.merchant_id).append('/items/products/generic')
        def conn = null
        try{
            HttpHeaders headers = new HttpHeaders()
            String token =  requestAccessToken(clientConfig)
            String authz = 'Bearer ' + token
            headers.setHeader('Authorization', authz)
            conn = client.doGet([debug:debug], buffer.toString(), [:], headers)
            if(conn.responseCode == 200){
                GPathResult result = client.parseTextAsXML([debug:debug], conn)
                        .declareNamespace(sc: 'http://schemas.google.com/structuredcontent/2009')
                def items = []
                result.'*'.findAll{it.name() == 'entry'}.each {entry ->
                    items << parseEntry(entry as GPathResult)
                }
                response = new SearchResponse(hits: items, total: items.size())
            }
            else{
                println client.getText([debug:debug], conn)
            }
        }
        finally {
            client.closeConnection(conn)
        }
        response
    }

    static String requestAccessToken(ClientConfig config, String scope = CONTENT_API_SERVICE) throws GeneralSecurityException, IOException {
        if(config.credentials?.refreshToken()){
            def params = [grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: generateJWT(config, scope)]
            def conn = null
            def map = [:]
            try{
                boolean debug = config.debug
                conn = client.doPost([debug:debug], 'https://www.googleapis.com/oauth2/v3/token', params)
                map = client.parseTextAsJSON([debug:debug], conn)
            }
            finally{
                client.closeConnection(conn)
            }
            final accessToken = map['access_token'] as String
            if(accessToken){
                config.credentials?.client_token = accessToken
                Calendar cal = Calendar.getInstance()
                cal.add(Calendar.SECOND, 3600)
                config.credentials?.expiration = cal.getTime()
            }
        }
        config.credentials?.client_token
    }

    static Item parseEntry(GPathResult entry){
        def item = new Item()
        def properties = [:]
        item.id = entry.'*'.find{it.name() == 'attribute' && it.@name == 'id'}.text().trim()
        properties.put('title', entry.title?.text()?.trim())
        properties.put('description', entry.content?.text()?.trim())
        entry.attribute.each {c ->
            parseProperty(c as GPathResult, properties)
        }
        item.map = properties
        item
    }

    static Map parseProperty(GPathResult property, Map properties){
        def k = property.'@name'?.text()?.trim()?.split(' ')?.join('_')
        if(k){
            properties.put(k, property.text())
        }
        k = property.@group?.text()?.trim()?.split(' ')?.join('_')
        if(k){
            def m = [:]
            property.attribute.each{c ->
                parseProperty(c as GPathResult, m)
            }
            properties.put(k, m)
        }
        properties
    }

    static String generateJWT(ClientConfig config, String scope = "https://www.googleapis.com/auth/prediction"){
        final clientId = config.credentials?.client_id
        final clientSecret = config.credentials?.client_secret

        def input = new StringBuilder()

        def json = new JsonBuilder()
        json alg: "RS256", typ: "JWT"
        input.append(new String(Base64.encodeBase64(json.toString().getBytes("UTF-8"))))

        final iat = (Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime().getTime() / 1000) as int
        json = new JsonBuilder()
        json iss: clientId,
                scope: scope,
                aud: "https://www.googleapis.com/oauth2/v3/token",
                exp: iat + 3600,
                iat: iat
        input.append(".").append(new String(Base64.encodeBase64(json.toString().getBytes("UTF-8"))))

        def key = clientSecret.replace("-----BEGIN PRIVATE KEY-----\n", "").replace("\n-----END PRIVATE KEY-----\n", "")
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(key.bytes))
        KeyFactory keyFactory = KeyFactory.getInstance("RSA")
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec)
        Signature sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(privateKey)
        sig.update(input.toString().bytes)
        byte[] signature = Base64.encodeBase64(sig.sign())

        input.append(".").append(new String(signature)).toString()
    }
}

