package tech.cryptonomic.conseil.generic.chain

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import tech.cryptonomic.conseil.generic.chain.DataTypes.OperationType.OperationType
import tech.cryptonomic.conseil.generic.chain.DataTypes.OrderDirection.OrderDirection

import scala.util.Try

/**
  * Classes used for deserializing query.
  */
object DataTypes {

  /** Default value of limit parameter */
  val defaultLimitValue: Int = 10000

  /** Max value of limit parameter */
  val maxLimitValue: Int = 100000

  /** Class required for OperationType enum serialization */
  class OperationTypeRef extends TypeReference[OperationType.type]

  /** Class representing predicate */
  case class Predicate(
    field: String,
    @JsonScalaEnumeration(classOf[OperationTypeRef]) operation: OperationType,
    set: List[Any],
    inverse: Boolean = false,
    precision: Option[Int] = None
  )

  /** Enumeration for order direction */
  object OrderDirection extends Enumeration {
    type OrderDirection = Value
    val asc, desc = Value
  }

  /** Class required for Ordering enum serialization */
  class QueryOrderingRef extends TypeReference[OrderDirection.type]
  case class QueryOrdering(field: String, @JsonScalaEnumeration(classOf[QueryOrderingRef]) direction: OrderDirection)

  /** Class representing query */
  case class Query(
    fields: List[String] = List.empty,
    predicates: List[Predicate],
    orderBy: List[QueryOrdering] = List.empty,
    limit: Option[Int] = Some(defaultLimitValue)
  ) {
    /** Method which validates query fields, as jackson runs on top of runtime reflection so NPE can happen if fields are missing */
    def validate: Option[Query] = {
      Try {
        predicates.foreach { pred =>
          pred.field.nonEmpty && OperationType.values.contains(pred.operation) && pred.set.nonEmpty
        }
        this
      }.toOption
    }
  }

  /** Enumeration of operation types */
  object OperationType extends Enumeration {
    type OperationType = Value
    val in, between, like, lt, gt, eq, startsWith, endsWith, before, after = Value
  }

}