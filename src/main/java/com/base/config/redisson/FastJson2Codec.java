package com.base.config.redisson;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.ContextAutoTypeBeforeHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import lombok.NoArgsConstructor;
import org.redisson.client.codec.Codec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor
public class FastJson2Codec implements Codec {

    private List<String> autotypePackages = Collections.emptyList();

    public FastJson2Codec(List<String> autotypePackages) {
        if (autotypePackages != null) {
            this.autotypePackages = autotypePackages;
        }
    }

    private final Encoder encoder = in -> {
        ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
        try {
            ByteBufOutputStream os = new ByteBufOutputStream(out);
            JSON.writeTo(os, in, JSONWriter.Feature.WriteClassName);
            return os.buffer();
        } catch (Exception e) {
            out.release();
            throw new IOException(e);
        }
    };

    private final Decoder<Object> decoder = (buf, state) -> {
        if (buf == null) {
            return null;
        }

        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);

        String str = new String(bytes, StandardCharsets.UTF_8);
        ContextAutoTypeBeforeHandler filter = autotypePackages.isEmpty()
                ? new ContextAutoTypeBeforeHandler(true)
                : new ContextAutoTypeBeforeHandler(true, autotypePackages.toArray(new String[0]));
        return JSON.parseObject(
                str,
                Object.class,
                filter,
                JSONReader.Feature.SupportClassForName);
    };

    private final Encoder encoderString = in -> {
        ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
        out.writeCharSequence(in.toString(), StandardCharsets.UTF_8);
        return out;
    };

    private final Decoder<Object> decoderString = (buf, state) -> {
        if (buf == null) {
            return null;
        }
        String str = buf.toString(StandardCharsets.UTF_8);
        buf.readerIndex(buf.readableBytes());
        return str;
    };

    @Override
    public Decoder<Object> getValueDecoder() {
        return decoder;
    }

    @Override
    public Encoder getValueEncoder() {
        return encoder;
    }

    @Override
    public Decoder<Object> getMapValueDecoder() {
        return getValueDecoder();
    }

    @Override
    public Encoder getMapValueEncoder() {
        return getValueEncoder();
    }

    @Override
    public Decoder<Object> getMapKeyDecoder() {
        return decoderString;
    }

    @Override
    public Encoder getMapKeyEncoder() {
        return encoderString;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
