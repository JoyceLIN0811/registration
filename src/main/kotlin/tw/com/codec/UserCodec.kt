package tw.com.codec

import com.mongodb.MongoClientSettings
import org.bson.*
import org.bson.codecs.CollectibleCodec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.types.ObjectId
import tw.com.MemberRepo.User

class UserCodec: CollectibleCodec<User>  {

    private var documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document::class.java)

    override fun encode(writer: BsonWriter?, user: User, encoderContext: EncoderContext?) {
        val doc = Document()
        doc["_id"] = user._id
        doc["userId"] = user.userId?:""
        doc["username"] = user.username?:""
        doc["createDate"] = user.createDate
        documentCodec!!.encode(writer, doc, encoderContext)
    }

    override fun getEncoderClass(): Class<User> {
        return User::class.java
    }

    override fun generateIdIfAbsentFromDocument(document: User): User? {
        if (!documentHasId(document)) {
            document._id = ObjectId()
        }
        return document
    }

    override fun documentHasId(document: User): Boolean {
        return document._id != null
    }

    override fun getDocumentId(document: User): BsonValue? {
        return BsonString(document._id.toString())
    }

    override fun decode(reader: BsonReader?, decoderContext: DecoderContext?): User? {
        val document = documentCodec.decode(reader, decoderContext)
        val user = User()
        if (document.getObjectId("_id") != null) {
            user._id = document.getObjectId("_id")
        }
        user.userId = document.getString("userId")?:""
        user.username = document.getString("username")?:""
        user.createDate = document.getDate("createDate")
        return user
    }
}