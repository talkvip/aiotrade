package ru.circumflex.orm

import ORM._
import java.sql.ResultSet
import ru.circumflex.orm.Constraint.PhysicalPrimaryKey
import ru.circumflex.orm.Constraint.VirtualPrimaryKey

/**
 * Result set projection.
 */
trait Projection[T] extends SQLable {
  /**
   * Extract a value from result set.
   */
  def read(rs: ResultSet): Option[T]

  /**
   * Return true for aggregate projections.
   * If at least one grouping projection presents in query, then all non-grouping projections
   * should appear to GROUP BY clause.
   */
  def grouping_?(): Boolean = false

  /**
   * Returns the list of aliases, from which this projection is composed.
   */
  def sqlAliases: Seq[String]

}

object Projection {
  /**
   * Defines a contract for single-column projections with assigned aliases
   * so that they may be used in ORDER BY/HAVING clauses.
   */
  trait AtomicProjection[T] extends Projection[T] {
    /**
     * Returns projection's alias
     */
    var alias: String = "this"

    def read(rs: ResultSet) = typeConverter.read(rs, alias).asInstanceOf[Option[T]]

    /**
     * Change an alias of this projection.
     */
    def as(alias: String): this.type = {
      this.alias = alias
      return this
    }

    def sqlAliases = List(alias)
  }

  /**
   * Defines a contract for multi-column projections that should be
   * read as a composite object.
   */
  trait CompositeProjection[R] extends Projection[R] {

    private var _hash = 0;

    def subProjections: Seq[Projection[_]]

    def sqlAliases = subProjections.flatMap(_.sqlAliases)

    override def equals(obj: Any) = obj match {
      case p: CompositeProjection[R] => (this.subProjections.toList -- p.subProjections.toList) == Nil
      case _ => false
    }

    override def hashCode: Int = {
      if (_hash == 0)
        for (p <- subProjections)
          _hash = 31 * _hash + p.hashCode
      return _hash
    }

    def toSql = subProjections.map(_.toSql).mkString(", ")

  }

  class ScalarProjection[T](val expression: String,
                            val grouping: Boolean)
  extends AtomicProjection[T] {

    override def grouping_?() = grouping

    def toSql = dialect.scalarAlias(expression, alias)

    override def equals(obj: Any) = obj match {
      case p: ScalarProjection[T] => p.expression == this.expression && p.grouping == this.grouping
      case _ => false
    }

    override def hashCode = expression.hashCode * 31 + grouping.hashCode
  }

  /**
   * Represents a projection for single field of a record.
   */
  class ColumnProjection[T, R](val node: RelationNode[R],
                               val column: Column[T, R])
  extends AtomicProjection[T] {

    /**
     * Returns a column name qualified with node's alias.
     */
    def expr = dialect.qualifyColumn(column, node.alias)

    def toSql = dialect.columnAlias(column, alias, node.alias)

    override def equals(obj: Any) = obj match {
      case p: ColumnProjection[T, R] => p.node == this.node && p.column == this.column
      case _ => false
    }

    override def hashCode = node.hashCode * 31 + column.hashCode
  }

  /**
   * Represents a record projection (it groups all field projections).
   */
  class RecordProjection[R](val node: RelationNode[R])
  extends CompositeProjection[R] {

    protected val _columnProjections: Seq[ColumnProjection[Any, R]] = node
    .relation
    .columns
    .map(col => new ColumnProjection(node, col.asInstanceOf[Column[Any, R]]))

    def subProjections = _columnProjections

    def read(rs: ResultSet): Option[R] = node.primaryKey match {
      case pk: PhysicalPrimaryKey[_, R] =>
        val pkColumn = node.primaryKey.column
        _columnProjections.find(_.column == pkColumn) match {
          case Some(pkProj) => pkProj.read(rs) match {
              case Some(idValue) =>
                tx.getCachedRecord(node.relation, idValue) match {
                  case Some(r: R) => Some(r)    // return cached record
                  case _ => readRecord(rs)
                }
              case _ => None
            }
          case _ => None
        }
      case v: VirtualPrimaryKey[R] => readRecord(rs)
      case _ => None
    }

    protected def readRecord(rs: ResultSet): Option[R] = {
      val record = node.relation.newRecordInstance.asInstanceOf[Record[R]]
      _columnProjections.foreach(p => record.setField(p.column, p.read(rs)))
      if (record.identified_?) {
        tx.updateRecordCache(record)
        Some(record.asInstanceOf[R])
      } else None
    }

    override def equals(obj: Any) = obj match {
      case p: RecordProjection[R] => this.node == p.node
      case _ => false
    }

    override def hashCode = node.hashCode

  }
}