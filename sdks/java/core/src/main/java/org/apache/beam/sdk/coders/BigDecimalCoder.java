/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.sdk.coders;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * A {@link BigDecimalCoder} encodes a {@link BigDecimal} as an integer scale encoded with
 * {@link VarIntCoder} and a {@link BigInteger} encoded using {@link BigIntegerCoder}. The
 * {@link BigInteger}, when scaled (with unlimited precision, aka {@link MathContext#UNLIMITED}),
 * yields the expected {@link BigDecimal}.
 */
public class BigDecimalCoder extends AtomicCoder<BigDecimal> {

  @JsonCreator
  public static BigDecimalCoder of() {
    return INSTANCE;
  }

  /////////////////////////////////////////////////////////////////////////////

  private static final BigDecimalCoder INSTANCE = new BigDecimalCoder();

  private final VarIntCoder integerCoder = VarIntCoder.of();
  private final BigIntegerCoder bigIntegerCoder = BigIntegerCoder.of();

  private BigDecimalCoder() {}

  @Override
  public void encode(BigDecimal value, OutputStream outStream, Context context)
      throws IOException, CoderException {
    checkNotNull(value, String.format("cannot encode a null %s", BigDecimal.class.getSimpleName()));
    integerCoder.encode(value.scale(), outStream, context.nested());
    bigIntegerCoder.encode(value.unscaledValue(), outStream, context.nested());
  }

  @Override
  public BigDecimal decode(InputStream inStream, Context context)
      throws IOException, CoderException {
    int scale = integerCoder.decode(inStream, context.nested());
    BigInteger bigInteger = bigIntegerCoder.decode(inStream, context.nested());
    return new BigDecimal(bigInteger, scale);
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code true}. This coder is injective.
   */
  @Override
  public boolean consistentWithEquals() {
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code true}, because {@link #getEncodedElementByteSize} runs in constant time.
   */
  @Override
  public boolean isRegisterByteSizeObserverCheap(BigDecimal value, Context context) {
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code 4} (the size of an integer denoting the scale) plus {@code 4} (the size of an
   * integer length prefix for the following bytes) plus the size of the two's-complement
   * representation of the {@link BigInteger} that, when scaled, equals the given value.
   */
  @Override
  protected long getEncodedElementByteSize(BigDecimal value, Context context) throws Exception {
    checkNotNull(value, String.format("cannot encode a null %s", BigDecimal.class.getSimpleName()));
    return integerCoder.getEncodedElementByteSize(value.scale(), context.nested())
        + bigIntegerCoder.getEncodedElementByteSize(value.unscaledValue(), context.nested());
  }
}