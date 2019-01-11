package tech.cryptonomic.conseil.util

import org.scalatest.{WordSpec, Matchers, EitherValues}
import tech.cryptonomic.conseil.tezos.TezosTypes._

class JsonDecodersTest extends WordSpec with Matchers with EitherValues {

  import JsonDecoders.Circe._
  import io.circe.parser.decode

  "the json decoders" should {

    val validHash = "signiRfcqmbGc6UtW1WzuJNGzRRsWDLpafxZZPwwTMntFwup8rTxXEgcLD5UBWkYmMqZECVEr33Xw5sh9NVi45c4FVAXvQSf"
    val invalidHash = "signiRfcqmbGc6UtW1WzuJNGzRRsWDLpafxZZPwwTMntFwup8rTxXEgcLD5UBWkYmMqZECVEr33Xw5sh9NVi45c4FVAXvQSl"

    /** wrap in quotes to be a valid json string */
    val jsonStringOf = (content: String) => s""""$content""""

    "decode valid json base58check strings into a PublicKeyHash" in {
      val decoded = decode[PublicKeyHash](jsonStringOf(validHash))
      decoded.right.value shouldBe PublicKeyHash(validHash)
    }

    "fail to decode an invalid json base58check strings into a PublicKeyHash" in {
      val decoded = decode[PublicKeyHash](jsonStringOf(invalidHash))
      decoded shouldBe 'left
    }

    "decode valid json base58check strings into a Signature" in {
      val decoded = decode[Signature](jsonStringOf(validHash))
      decoded.right.value shouldBe Signature(validHash)
    }

    "fail to decode an invalid json base58check strings into a Signature" in {
      val decoded = decode[Signature](jsonStringOf(invalidHash))
      decoded shouldBe 'left
    }

    "decode valid json base58check strings into a BlockHash" in {
      val decoded = decode[BlockHash](jsonStringOf(validHash))
      decoded.right.value shouldBe BlockHash(validHash)
    }

    "fail to decode an invalid json base58check strings into a BlockHash" in {
      val decoded = decode[BlockHash](jsonStringOf(invalidHash))
      decoded shouldBe 'left
    }

    "decode valid json base58check strings into a OperationHash" in {
      val decoded = decode[OperationHash](jsonStringOf(validHash))
      decoded.right.value shouldBe OperationHash(validHash)
    }

    "fail to decode an invalid json base58check strings into a OperationHash" in {
      val decoded = decode[OperationHash](jsonStringOf(invalidHash))
      decoded shouldBe 'left
    }

    "decode valid json base58check strings into a AccountId" in {
      val decoded = decode[AccountId](jsonStringOf(validHash))
      decoded.right.value shouldBe AccountId(validHash)
    }

    "fail to decode an invalid json base58check strings into a AccountId" in {
      val decoded = decode[AccountId](jsonStringOf(invalidHash))
      decoded shouldBe 'left
    }

    "decode valid json base58check strings into a ContractId" in {
      val decoded = decode[ContractId](jsonStringOf(validHash))
      decoded.right.value shouldBe ContractId(validHash)
    }

    "fail to decode an invalid json base58check strings into a ContractId" in {
      val decoded = decode[ContractId](jsonStringOf(invalidHash))
      decoded shouldBe 'left
    }

    val endorsementJson =
      """{
        |  "kind": "endorsement",
        |  "level": 182308,
        |  "metadata": {
        |      "balance_updates": [
        |          {
        |              "kind": "contract",
        |              "contract": "tz1fyvFH2pd3V9UEq5psqVokVBYkt7rHTKio",
        |              "change": "-256000000"
        |          },
        |          {
        |              "kind": "freezer",
        |              "category": "deposits",
        |              "delegate": "tz1fyvFH2pd3V9UEq5psqVokVBYkt7rHTKio",
        |              "level": 1424,
        |              "change": "256000000"
        |          },
        |          {
        |              "kind": "freezer",
        |              "category": "rewards",
        |              "delegate": "tz1fyvFH2pd3V9UEq5psqVokVBYkt7rHTKio",
        |              "level": 1424,
        |              "change": "4000000"
        |          }
        |      ],
        |      "delegate": "tz1fyvFH2pd3V9UEq5psqVokVBYkt7rHTKio",
        |      "slots": [
        |          29,
        |          27,
        |          20,
        |          17
        |      ]
        |  }
        |}""".stripMargin

    val expectedEndorsement =
          TezosOperation.Endorsement(
            level = 182308,
            metadata = TezosOperation.EndorsementMetadata(
              slots =List(29, 27, 20, 17),
              delegate = PublicKeyHash("tz1fyvFH2pd3V9UEq5psqVokVBYkt7rHTKio"),
              balance_updates = List(
                TezosOperation.OperationMetadata.BalanceUpdate(
                  kind = "contract",
                  contract = Some(ContractId("tz1fyvFH2pd3V9UEq5psqVokVBYkt7rHTKio")),
                  change = -256000000,
                  category = None,
                  delegate = None,
                  level = None
                ),
                TezosOperation.OperationMetadata.BalanceUpdate(
                  kind = "freezer",
                  category = Some("deposits"),
                  delegate = Some(PublicKeyHash("tz1fyvFH2pd3V9UEq5psqVokVBYkt7rHTKio")),
                  change = 256000000,
                  contract = None,
                  level = Some(1424)
                ),
                TezosOperation.OperationMetadata.BalanceUpdate(
                  kind = "freezer",
                  category = Some("rewards"),
                  delegate = Some(PublicKeyHash("tz1fyvFH2pd3V9UEq5psqVokVBYkt7rHTKio")),
                  change = 4000000,
                  contract = None,
                  level = Some(1424)
                )
              )
          )
        )

    "decode an endorsement operation from json" in {
      val decoded = decode[TezosOperation.Endorsement](endorsementJson)
      decoded shouldBe 'right

      val endorsement = decoded.right.value
      endorsement shouldBe a [TezosOperation.Endorsement]
      endorsement shouldEqual expectedEndorsement

    }
  }

}