package com.tacz.guns.resource.serialize;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;

import java.io.IOException;

/**
 * 在 {@link Modifier} 反序列化时预编译内联函数（1.2.0 引入）。
 *
 * <p>为什么必须挂在 Gson 上：配件数据有两条入口 —— 数据包加载（{@code CommonAssetsManager}
 * 的 GSON）和网络同步（服务端把 JSON 塞进枪包缓存，客户端再解析）。挂在 type adapter 上
 * 两条路径都覆盖；若改在某个 loader 里手动调用，客户端同步来的对象就没有编译结果，
 * 表现为「单人正常、进服务器后配件属性不对」。
 *
 * <p>只有 {@code Modifier} 自己走这个 adapter，其它类型原样委托（返回 {@code null} 表示不接管）。
 */
public class ModifierTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type.getRawType() != Modifier.class) {
            return null;
        }
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                T value = delegate.read(in);
                if (value instanceof Modifier modifier) {
                    modifier.compileFunction();
                }
                return value;
            }
        };
    }
}
