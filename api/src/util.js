const jwt = require('jsonwebtoken');

const wrap = (fn) => (req, res, next) => Promise.resolve(fn(req, res, next)).catch(next);

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function auth(...roles) {
  return (req, res, next) => {
    const header = req.headers.authorization || '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : null;
    if (!token) return res.status(401).json({ error: 'Falta el token' });
    try {
      req.user = jwt.verify(token, process.env.JWT_SECRET);
    } catch {
      return res.status(401).json({ error: 'Token inválido o vencido' });
    }
    if (roles.length && !roles.includes(req.user.rol)) {
      return res.status(403).json({ error: 'Sin permiso para esta acción' });
    }
    next();
  };
}

function requireFields(body, fields) {
  const faltan = fields.filter((f) => body[f] === undefined || body[f] === null || body[f] === '');
  if (faltan.length) throw new HttpError(400, `Faltan campos: ${faltan.join(', ')}`);
}

module.exports = { wrap, HttpError, auth, requireFields };
