// guard(x, y, z, ...) { return }
inline fun guard(vararg args: Any?, contingency:()->Unit) {
	for(arg in args) {
		if(arg == null) {
			contingency()
			throw IllegalStateException("Guard contingency block must exit calling context")
		}
	}
}

// guard(x, where = {...}) { return }
inline fun unless(cond: Boolean, contingency: () -> Unit) {
	if(!cond) contingency()
}
inline fun <T> guard(arg: T?, where: (T)->Boolean, contingency: ()->Unit) {
	unless(arg != null && where(arg)) {
		contingency()
		throw IllegalStateException("Guard contingency block must exit calling context")
	}
}

// x.guard {...} .fail{...}
class Failure(val failed: Boolean) {
	inline fun fail(failBlock: ()->Unit) {
		if(failed) {
			failBlock()
			throw IllegalStateException("Fail block must exit calling context")
		}
	}
}
inline fun <T> T?.guard( block: T.()->Unit): Failure {
	val result = this?.block()
	return Failure(result == null)
}