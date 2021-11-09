package org.drools.brms.server.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;

import org.drools.repository.AssetItem;
import org.drools.repository.RulesRepositoryException;

public class RowLoader {

    private String[] headers;
    List             extractors;

    public String[] getHeaders() {
        return headers;
    }

    public String[] getRow(AssetItem item) {
        String[] row = new String[headers.length];
        for ( int i = 0; i < row.length; i++ ) {
            Method meth = (Method) extractors.get( i );
            try {
                Object val = meth.invoke( item,
                                          (Object[]) null );
                if ( val instanceof String ) {
                    String s = (String) val;
                    if ( s.length() > 64 ) {
                        s = s.substring( 0,
                                         61 ) + "...";
                    }
                    row[i] = s;

                } else if ( val instanceof Calendar ) {
                    row[i] = DateFormat.getDateInstance().format( ((Calendar) val).getTime() );
                } else {
                    row[i] = val.toString();
                }
            } catch ( Exception e ) {
                if ( e instanceof RuntimeException ) throw (RuntimeException) e;
                throw new RulesRepositoryException( e );
            }
        }
        return row;
    }

    public RowLoader(String resourcename) {

        InputStream in = RowLoader.class.getResourceAsStream( "/" + resourcename + ".properties" );
        BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
        List fields = new ArrayList();
        extractors = new ArrayList();
        String line;

        try {

            while ( (line = reader.readLine()) != null ) {
                if ( !line.startsWith( "#" ) && !line.trim().equals( "" ) ) {
                    StringTokenizer tok = new StringTokenizer( line,
                                                               "=" );
                    String field = tok.nextToken();
                    String method = tok.nextToken();
                    fields.add( field );

                    final Method meth = AssetItem.class.getMethod( method,
                                                                   new Class[]{} );

                    extractors.add( meth );

                }
            }
        } catch ( Exception e ) {
            if ( e instanceof RuntimeException ) {
                throw (RuntimeException) e;
            }
            throw new RulesRepositoryException( e );
        } finally {
            closeStream( reader );
        }
        headers = (String[]) fields.toArray( new String[fields.size()] );
    }

    private void closeStream(BufferedReader reader) {
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
