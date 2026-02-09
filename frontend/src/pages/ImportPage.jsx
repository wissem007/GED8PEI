import React, { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Paper, Typography, Button, LinearProgress, Alert,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Chip, Card, CardContent, List, ListItem, ListItemIcon, ListItemText
} from '@mui/material';
import {
  CloudUpload as UploadIcon, CheckCircle as SuccessIcon,
  Error as ErrorIcon, Warning as WarningIcon,
  Description as FileIcon, Info as InfoIcon
} from '@mui/icons-material';
import { useDropzone } from 'react-dropzone';
import { importAPI } from '../services/api';

function ImportPage() {
  const navigate = useNavigate();
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const onDrop = useCallback((acceptedFiles) => {
    if (acceptedFiles.length > 0) {
      setFile(acceptedFiles[0]);
      setResult(null);
      setError(null);
    }
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'text/csv': ['.csv'],
      'application/vnd.ms-excel': ['.xls'],
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx']
    },
    maxFiles: 1
  });

  const handleUpload = async () => {
    if (!file) return;

    try {
      setUploading(true);
      setError(null);
      setProgress(0);

      const formData = new FormData();
      formData.append('file', file);

      const response = await importAPI.upload(formData, {
        onUploadProgress: (progressEvent) => {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          setProgress(percent);
        }
      });

      setResult(response.data);
    } catch (err) {
      console.error('Erreur import:', err);
      setError(err.response?.data?.message || 'Erreur lors de l\'import');
    } finally {
      setUploading(false);
    }
  };

  const getStatusChip = (status) => {
    switch (status) {
      case 'COMPLETED':
        return <Chip icon={<SuccessIcon />} label="Terminé" color="success" />;
      case 'COMPLETED_WITH_ERRORS':
        return <Chip icon={<WarningIcon />} label="Terminé avec erreurs" color="warning" />;
      case 'FAILED':
        return <Chip icon={<ErrorIcon />} label="Échoué" color="error" />;
      default:
        return <Chip label={status} />;
    }
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <Box>
      <Typography variant="h5" gutterBottom>
        Import de fichiers
      </Typography>

      {/* Instructions */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            <InfoIcon sx={{ verticalAlign: 'middle', mr: 1 }} />
            Instructions
          </Typography>
          <List dense>
            <ListItem>
              <ListItemIcon><FileIcon /></ListItemIcon>
              <ListItemText
                primary="Formats acceptés"
                secondary="CSV (.csv), Excel (.xls, .xlsx)"
              />
            </ListItem>
            <ListItem>
              <ListItemIcon><CheckCircle /></ListItemIcon>
              <ListItemText
                primary="Détection automatique"
                secondary="Les colonnes sont détectées automatiquement (hostname, IP, environnement, etc.)"
              />
            </ListItem>
            <ListItem>
              <ListItemIcon><WarningIcon color="warning" /></ListItemIcon>
              <ListItemText
                primary="Mise à jour"
                secondary="Les serveurs existants seront mis à jour si le hostname correspond"
              />
            </ListItem>
          </List>
        </CardContent>
      </Card>

      {/* Zone de drop */}
      <Paper
        {...getRootProps()}
        sx={{
          p: 4,
          mb: 3,
          textAlign: 'center',
          cursor: 'pointer',
          backgroundColor: isDragActive ? 'action.hover' : 'background.paper',
          border: '2px dashed',
          borderColor: isDragActive ? 'primary.main' : 'divider',
          '&:hover': {
            backgroundColor: 'action.hover',
            borderColor: 'primary.main'
          }
        }}
      >
        <input {...getInputProps()} />
        <UploadIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
        <Typography variant="h6" gutterBottom>
          {isDragActive ? 'Déposez le fichier ici...' : 'Glissez-déposez un fichier ici'}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          ou cliquez pour sélectionner
        </Typography>
      </Paper>

      {/* Fichier sélectionné */}
      {file && (
        <Paper sx={{ p: 2, mb: 3 }}>
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Box display="flex" alignItems="center" gap={2}>
              <FileIcon color="primary" />
              <Box>
                <Typography variant="body1">{file.name}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {formatFileSize(file.size)}
                </Typography>
              </Box>
            </Box>
            <Box display="flex" gap={2}>
              <Button
                variant="outlined"
                onClick={() => {
                  setFile(null);
                  setResult(null);
                }}
              >
                Annuler
              </Button>
              <Button
                variant="contained"
                startIcon={<UploadIcon />}
                onClick={handleUpload}
                disabled={uploading}
              >
                Importer
              </Button>
            </Box>
          </Box>
          {uploading && (
            <Box sx={{ mt: 2 }}>
              <LinearProgress variant="determinate" value={progress} />
              <Typography variant="body2" color="text.secondary" align="center" sx={{ mt: 1 }}>
                {progress}%
              </Typography>
            </Box>
          )}
        </Paper>
      )}

      {/* Erreur */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Résultat */}
      {result && (
        <Paper sx={{ p: 3 }}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6">Résultat de l'import</Typography>
            {getStatusChip(result.status)}
          </Box>

          <TableContainer>
            <Table size="small">
              <TableBody>
                <TableRow>
                  <TableCell>Fichier</TableCell>
                  <TableCell>{result.filename}</TableCell>
                </TableRow>
                <TableRow>
                  <TableCell>Type</TableCell>
                  <TableCell>{result.fileType}</TableCell>
                </TableRow>
                <TableRow>
                  <TableCell>Lignes totales</TableCell>
                  <TableCell>{result.totalRows}</TableCell>
                </TableRow>
                <TableRow>
                  <TableCell>Lignes importées</TableCell>
                  <TableCell>
                    <Chip label={result.importedRows} color="success" size="small" />
                  </TableCell>
                </TableRow>
                {result.skippedRows > 0 && (
                  <TableRow>
                    <TableCell>Lignes ignorées</TableCell>
                    <TableCell>
                      <Chip label={result.skippedRows} color="warning" size="small" />
                    </TableCell>
                  </TableRow>
                )}
                {result.errorRows > 0 && (
                  <TableRow>
                    <TableCell>Lignes en erreur</TableCell>
                    <TableCell>
                      <Chip label={result.errorRows} color="error" size="small" />
                    </TableCell>
                  </TableRow>
                )}
                <TableRow>
                  <TableCell>Durée</TableCell>
                  <TableCell>{result.durationMs} ms</TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </TableContainer>

          {result.errorMessage && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              {result.errorMessage}
            </Alert>
          )}

          <Box display="flex" gap={2} mt={3}>
            <Button
              variant="contained"
              onClick={() => navigate('/servers')}
            >
              Voir les serveurs
            </Button>
            <Button
              variant="outlined"
              onClick={() => {
                setFile(null);
                setResult(null);
              }}
            >
              Nouvel import
            </Button>
          </Box>
        </Paper>
      )}
    </Box>
  );
}

// Helper component
function CheckCircle() {
  return <SuccessIcon color="success" />;
}

export default ImportPage;
