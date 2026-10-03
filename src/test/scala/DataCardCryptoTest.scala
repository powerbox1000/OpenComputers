import java.security.{KeyPairGenerator, SecureRandom, Signature}
import javax.crypto.KeyAgreement

import li.cil.oc.server.component.DataCard.ECUserdata
import org.junit.runner.RunWith
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DataCardCryptoTest extends AnyFunSpec with Matchers {
  describe("Ed25519 data-card keys") {
    it("generates, serializes, restores, signs, and verifies keys") {
      val generator = KeyPairGenerator.getInstance("Ed25519")
      generator.initialize(255, new SecureRandom())
      val pair = generator.generateKeyPair()

      val publicKey = new ECUserdata(pair.getPublic)
      val privateKey = new ECUserdata(pair.getPrivate)
      publicKey.keyType should be(ECUserdata.Ed25519PublicTypeName)
      privateKey.keyType should be(ECUserdata.Ed25519PrivateTypeName)

      val restoredPublic = ECUserdata.deserializeKey(publicKey.keyType, publicKey.value.getEncoded)
      val restoredPrivate = ECUserdata.deserializeKey(privateKey.keyType, privateKey.value.getEncoded)
      restoredPublic.getEncoded.toSeq should be(publicKey.value.getEncoded.toSeq)
      restoredPrivate.getEncoded.toSeq should be(privateKey.value.getEncoded.toSeq)

      val message = "OC data card signature".getBytes("UTF-8")
      val signer = Signature.getInstance("Ed25519")
      signer.initSign(restoredPrivate.asInstanceOf[java.security.PrivateKey])
      signer.update(message)
      val signature = signer.sign()

      val verifier = Signature.getInstance("Ed25519")
      verifier.initVerify(restoredPublic.asInstanceOf[java.security.PublicKey])
      verifier.update(message)
      verifier.verify(signature) should be(true)

      verifier.initVerify(restoredPublic.asInstanceOf[java.security.PublicKey])
      verifier.update("altered message".getBytes("UTF-8"))
      verifier.verify(signature) should be(false)
    }
  }

  describe("X25519 data-card keys") {
    it("generates, serializes, restores, and derives the same secret on both sides") {
      def keyPair() = {
        val generator = KeyPairGenerator.getInstance("X25519")
        generator.initialize(255, new SecureRandom())
        generator.generateKeyPair()
      }

      val alice = keyPair()
      val bob = keyPair()
      val alicePublic = new ECUserdata(alice.getPublic)
      val alicePrivate = new ECUserdata(alice.getPrivate)
      val bobPublic = new ECUserdata(bob.getPublic)
      val bobPrivate = new ECUserdata(bob.getPrivate)

      alicePublic.keyType should be(ECUserdata.X25519PublicTypeName)
      alicePrivate.keyType should be(ECUserdata.X25519PrivateTypeName)

      val restoredAlicePublic = ECUserdata.deserializeKey(alicePublic.keyType, alicePublic.value.getEncoded)
      val restoredAlicePrivate = ECUserdata.deserializeKey(alicePrivate.keyType, alicePrivate.value.getEncoded)
      val restoredBobPublic = ECUserdata.deserializeKey(bobPublic.keyType, bobPublic.value.getEncoded)
      val restoredBobPrivate = ECUserdata.deserializeKey(bobPrivate.keyType, bobPrivate.value.getEncoded)

      def sharedSecret(privateKey: java.security.PrivateKey, publicKey: java.security.PublicKey): Array[Byte] = {
        val agreement = KeyAgreement.getInstance("X25519")
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)
        agreement.generateSecret()
      }

      sharedSecret(
        restoredAlicePrivate.asInstanceOf[java.security.PrivateKey],
        restoredBobPublic.asInstanceOf[java.security.PublicKey]
      ).toSeq should be(sharedSecret(
        restoredBobPrivate.asInstanceOf[java.security.PrivateKey],
        restoredAlicePublic.asInstanceOf[java.security.PublicKey]
      ).toSeq)
    }
  }
}
