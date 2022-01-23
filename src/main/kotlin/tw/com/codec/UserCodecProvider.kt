package tw.com.codec

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import tw.com.MemberRepo.User

class UserCodecProvider : CodecProvider {
    override fun <T> get(clazz: Class<T>, registry: CodecRegistry?): Codec<T>? {
        return if (clazz == User::class.java) {
            UserCodec() as Codec<T>?
        } else null
    }
}