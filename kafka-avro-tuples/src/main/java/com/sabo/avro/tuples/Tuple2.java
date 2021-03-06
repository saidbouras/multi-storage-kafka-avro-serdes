/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sabo.avro.tuples;

import org.apache.avro.Schema;

import java.util.Arrays;
import java.util.Collections;


/**
 * A _1/_2 tuple.
 */
public class Tuple2<T1, T2> extends Tuple {

    public static <T1, T2> Tuple2<T1, T2> of(T1 t1, T2 t2) {
        return new Tuple2<>(t1, t2);
    }

    public Tuple2(Schema schema) {
        super(schema);
    }

    public Tuple2(T1 _1, T2 _2) {
        name = this.getClass().getSimpleName();
        innerTuple = Collections.unmodifiableList(Arrays.asList(_1, _2));
    }

    /**
     * Get the _1.
     */
    public T1 v1() {
        return (T1) innerTuple.get(0);
    }

    /**
     * Get the _2.
     */
    public T2 v2() {
        return (T2) innerTuple.get(1);
    }

    public Tuple2<T1, T2> as(String name) {
        this.name = Tuple.class.getSimpleName() + name;
        return this;
    }
}