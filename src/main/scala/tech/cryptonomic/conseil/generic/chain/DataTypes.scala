package tech.cryptonomic.conseil.generic.chain

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import tech.cryptonomic.conseil.generic.chain.DataTypes.AggregationType.AggregationType
import tech.cryptonomic.conseil.generic.chain.DataTypes.OperationType.OperationType
import tech.cryptonomic.conseil.generic.chain.DataTypes.OrderDirection.OrderDirection
import tech.cryptonomic.conseil.generic.chain.DataTypes.OutputType.OutputType
import tech.cryptonomic.conseil.tezos.TezosPlatformDiscoveryOperations

import scala.concurrent.{ExecutionContext, Future}


/**
  * Classes used for deserializing query.
  */
object DataTypes {
  import io.scalaland.chimney.dsl._

  /** Type representing Map[String, Any] */
  type AnyMap = Map[String, Any]

  /** Type representing Map[String, Option[Any]] for query response */
  type QueryResponse = Map[String, Option[Any]]
  /** Default value of limit parameter */
  val defaultLimitValue: Int = 10000
  /** Max value of limit parameter */
  val maxLimitValue: Int = 100000

  /** Trait representing query validation errors */
  sealed trait QueryValidationError extends Product with Serializable {
    val message: String
  }

  /** Class which contains output type with the response */
  case class QueryResponseWithOutput(queryResponse: List[QueryResponse], output: OutputType)

  /** Class required for OperationType enum serialization */
  class OperationTypeRef extends TypeReference[OperationType.type]

  /** Class required for OutputType enum serialization */
  class OutputTypeRef extends TypeReference[OutputType.type]

  /** Class representing predicate */
  case class Predicate(
    field: String,
    @JsonScalaEnumeration(classOf[OperationTypeRef]) operation: OperationType,
    set: List[Any] = List.empty,
    inverse: Boolean = false,
    precision: Option[Int] = None
  )

  /** Class required for Ordering enum serialization */
  class QueryOrderingRef extends TypeReference[OrderDirection.type]

  /** Class representing query ordering */
  case class QueryOrdering(field: String, @JsonScalaEnumeration(classOf[QueryOrderingRef]) direction: OrderDirection)

  /** Class representing invalid query field */
  case class InvalidQueryField(message: String) extends QueryValidationError

  /** Class representing invalid predicate field */
  case class InvalidPredicateField(message: String) extends QueryValidationError

  /** Class representing invalid order by field */
  case class InvalidOrderByField(message: String) extends QueryValidationError

  /** Class representing query */
  case class Query(
    fields: List[String] = List.empty,
    predicates: List[Predicate] = List.empty,
    orderBy: List[QueryOrdering] = List.empty,
    limit: Int = defaultLimitValue,
    output: OutputType = OutputType.json,
    aggregation: Option[Aggregation] = None
  )


//  {
//    "fields": [],
//    ...
//    "aggregation": {
//      "field": "",
//      "function": "[sum|count|max|min|avg]",
//      "predicate": {
//      "operation": "operation",
//      "set": [],
//      "inverse": false,
//      "precision": 2
//    }
//    }
//  }
  case class Aggregation(field: String, function: AggregationType, predicate: Option[Predicate] = None)

  /** Class representing query got through the REST API */
  case class ApiQuery(
    fields: Option[List[String]],
    predicates: Option[List[Predicate]],
    orderBy: Option[List[QueryOrdering]],
    limit: Option[Int],
    @JsonScalaEnumeration(classOf[OutputTypeRef]) output: Option[OutputType],
    aggregation: Option[Aggregation]
  ) {
    /** Method which validates query fields */
    def validate(entity: String, tezosPlatformDiscovery: TezosPlatformDiscoveryOperations)(implicit ec: ExecutionContext):
    Future[Either[List[QueryValidationError], Query]] = {
      import cats.implicits._

      val query = Query().patchWith(this)

      val invalidQueryFields = query
        .fields
        .map(field => tezosPlatformDiscovery.areFieldsValid(entity, Set(field)).map(_ -> field))
        .sequence
        .map(_.filterNot { case (isValid, _) => isValid }.map { case (_, fieldName) => InvalidQueryField(fieldName) })

      val invalidPredicateFields = query
        .predicates
        .map(_.field)
        .map(field => tezosPlatformDiscovery.areFieldsValid(entity, Set(field)).map(_ -> field))
        .sequence
        .map(_.filterNot { case (isValid, _) => isValid }.map{ case (_, fieldName) => InvalidPredicateField(fieldName) })

      val invalidOrderByFields = query
        .orderBy
        .map(_.field)
        .map(field => tezosPlatformDiscovery.areFieldsValid(entity, Set(field)).map(_ -> field))
        .sequence
        .map(_.filterNot { case (isValid, _) => isValid }.map{ case (_, fieldName) => InvalidOrderByField(fieldName) })

      for {
        invQF <- invalidQueryFields
        invPF <- invalidPredicateFields
        invODBF <- invalidOrderByFields
      } yield {
        invQF ::: invPF ::: invODBF match {
          case Nil => Right(query)
          case wrongFields => Left(wrongFields)
        }
      }
    }
  }

  /** Enumeration for output types */
  object OutputType extends Enumeration {
    type OutputType = Value
    val json, csv = Value
  }

  /** Enumeration for order direction */
  object OrderDirection extends Enumeration {
    type OrderDirection = Value
    val asc, desc = Value
  }

  /** Enumeration of operation types */
  object OperationType extends Enumeration {
    type OperationType = Value
    val in, between, like, lt, gt, eq, startsWith, endsWith, before, after, isnull = Value
  }

  /** Enumeration of aggregation functions */
  object AggregationType extends Enumeration {
    type AggregationType = Value
    val sum, count, max, min, avg = Value
  }
}
