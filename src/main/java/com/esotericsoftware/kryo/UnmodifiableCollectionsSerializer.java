/*
 * Copyright 2010 Martin Grotzke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.esotericsoftware.kryo;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.esotericsoftware.kryo.serialize.IntSerializer;

/**
 * A kryo {@link Serializer} for unmodifiable {@link Collection}s and {@link Map}s
 * created via {@link Collections}.
 * 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
public class UnmodifiableCollectionsSerializer extends Serializer {
    
    private static final Field SOURCE_COLLECTION_FIELD;
    private static final Field SOURCE_MAP_FIELD;
    
    static {
        try {
            SOURCE_COLLECTION_FIELD = Class.forName("java.util.Collections$UnmodifiableCollection" )
                .getDeclaredField( "c" );
            SOURCE_COLLECTION_FIELD.setAccessible( true );
            

            SOURCE_MAP_FIELD = Class.forName("java.util.Collections$UnmodifiableMap" )
                .getDeclaredField( "m" );
            SOURCE_MAP_FIELD.setAccessible( true );
        } catch ( final Exception e ) {
            throw new RuntimeException( "Could not access source collection" +
                    " field in java.util.Collections$UnmodifiableCollection.", e );
        }
    }
    
    private final Kryo _kryo;
    
    /**
     * @param kryo the kryo instance
     */
    public UnmodifiableCollectionsSerializer( final Kryo kryo ) {
        _kryo = kryo;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public <T> T readObjectData( final ByteBuffer buffer, final Class<T> clazz ) {
        final int ordinal = IntSerializer.get( buffer, true );
        final UnmodifiableCollection unmodifiableCollection = UnmodifiableCollection.values()[ordinal];
        try {
            final Object sourceCollection = _kryo.readClassAndObject( buffer );
            return (T) unmodifiableCollection.create( sourceCollection );
        } catch ( final Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeObjectData( final ByteBuffer buffer, final Object object ) {
        try {
            final UnmodifiableCollection unmodifiableCollection = UnmodifiableCollection.valueOfType( object.getClass() );
            // the ordinal could be replaced by s.th. else (e.g. a explicitely managed "id")
            IntSerializer.put( buffer, unmodifiableCollection.ordinal(), true );
            _kryo.writeClassAndObject( buffer, unmodifiableCollection.sourceCollectionField.get( object ) );
        } catch ( final Exception e ) {
            throw new RuntimeException( e );
        }
    }
    
    private static enum UnmodifiableCollection {
        COLLECTION( Collections.unmodifiableCollection( Arrays.asList( "" ) ).getClass(), SOURCE_COLLECTION_FIELD ){
            @Override
            public Object create( final Object sourceCollection ) {
                return Collections.unmodifiableCollection( (Collection<?>) sourceCollection );
            }
        },
        RANDOM_ACCESS_LIST( Collections.unmodifiableList( new ArrayList<Void>() ).getClass(), SOURCE_COLLECTION_FIELD ){
            @Override
            public Object create( final Object sourceCollection ) {
                return Collections.unmodifiableList( (List<?>) sourceCollection );
            }
        },
        LIST( Collections.unmodifiableList( new LinkedList<Void>() ).getClass(), SOURCE_COLLECTION_FIELD ){
            @Override
            public Object create( final Object sourceCollection ) {
                return Collections.unmodifiableList( (List<?>) sourceCollection );
            }
        },
        SET( Collections.unmodifiableSet( new HashSet<Void>() ).getClass(), SOURCE_COLLECTION_FIELD ){
            @Override
            public Object create( final Object sourceCollection ) {
                return Collections.unmodifiableSet( (Set<?>) sourceCollection );
            }
        },
        SORTED_SET( Collections.unmodifiableSortedSet( new TreeSet<Void>() ).getClass(), SOURCE_COLLECTION_FIELD ){
            @Override
            public Object create( final Object sourceCollection ) {
                return Collections.unmodifiableSortedSet( (SortedSet<?>) sourceCollection );
            }
        },
        MAP( Collections.unmodifiableMap( new HashMap<Void, Void>() ).getClass(), SOURCE_MAP_FIELD ) {

            @Override
            public Object create( final Object sourceCollection ) {
                return Collections.unmodifiableMap( (Map<?, ?>) sourceCollection );
            }
            
        },
        SORTED_MAP( Collections.unmodifiableSortedMap( new TreeMap<Void, Void>() ).getClass(), SOURCE_MAP_FIELD ) {
            @Override
            public Object create( final Object sourceCollection ) {
                return Collections.unmodifiableSortedMap( (SortedMap<?, ?>) sourceCollection );
            }
        };
        
        private final Class<?> type;
        private final Field sourceCollectionField;
        
        private UnmodifiableCollection( final Class<?> type, final Field sourceCollectionField ) {
            this.type = type;
            this.sourceCollectionField = sourceCollectionField;
        }
        
        /**
         * @param sourceCollection
         */
        public abstract Object create( Object sourceCollection );

        static UnmodifiableCollection valueOfType( final Class<?> type ) {
            for( final UnmodifiableCollection item : values() ) {
                if ( item.type.equals( type ) ) {
                    return item;
                }
            }
            throw new IllegalArgumentException( "The type " + type + " is not supported." );
        }
        
    }

    /**
     * Creates a new {@link UnmodifiableCollectionsSerializer} and sets it as serializer
     * for the several unmodifiable Collections that can be created via {@link Collections},
     * including {@link Map}s.
     * 
     * @param kryo the {@link Kryo} instance to set the serializer on.
     * 
     * @see Collections#unmodifiableCollection(Collection)
     * @see Collections#unmodifiableList(List)
     * @see Collections#unmodifiableSet(Set)
     * @see Collections#unmodifiableSortedSet(SortedSet)
     * @see Collections#unmodifiableMap(Map)
     * @see Collections#unmodifiableSortedMap(SortedMap)
     */
    public static void setSerializer( final Kryo kryo ) {
        final UnmodifiableCollectionsSerializer serializer = new UnmodifiableCollectionsSerializer( kryo );
        UnmodifiableCollection.values();
        for ( final UnmodifiableCollection item : UnmodifiableCollection.values() ) {
            kryo.setSerializer( item.type, serializer );
        }
    }

}