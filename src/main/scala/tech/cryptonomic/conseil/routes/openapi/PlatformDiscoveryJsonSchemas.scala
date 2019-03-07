package tech.cryptonomic.conseil.routes.openapi

import endpoints.{algebra, generic}
import tech.cryptonomic.conseil.generic.chain.PlatformDiscoveryTypes._

/** Trait containing metadata endpoints JSON schemas */
trait PlatformDiscoveryJsonSchemas extends algebra.JsonSchemas with generic.JsonSchemas {

  /** Platform JSON schema */
  implicit def platformSchema: JsonSchema[Platform] =
    genericJsonSchema[Platform]

  /** Network JSON schema */
  implicit def networkSchema: JsonSchema[Network] =
    genericJsonSchema[Network]

  /** Entity JSON schema */
  implicit def entitySchema: JsonSchema[Entity] =
    genericJsonSchema[Entity]

  /** Attribute JSON schema */
  implicit def attributeSchema: JsonSchema[Attribute] =
    genericJsonSchema[Attribute]

  /** Data type JSON schema */
  implicit def dataTypeSchema: JsonSchema[DataType.Value] =
    enumeration(DataType.values.toSeq)(_.toString)

  /** Key type JSON schema */
  implicit def keyTypeSchema: JsonSchema[KeyType.Value] =
    enumeration(KeyType.values.toSeq)(_.toString)
}